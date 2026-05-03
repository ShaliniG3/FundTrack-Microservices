package com.cts.fundtrack.program.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.dto.RequiredDocumentDTO;
import com.cts.fundtrack.program.models.EligibilityRule;
import com.cts.fundtrack.program.models.Program;
import com.cts.fundtrack.program.models.RequiredDocument;


/**
 * Mapper interface for converting between {@link Program} entities and their corresponding DTOs.
 * <p>
 * Uses MapStruct for high-performance, compile-time type mapping. This mapper 
 * supports deep mapping of nested lists including eligibility rules and required documents.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ProgramMapper {

    /**
     * Converts a request DTO into a Program entity.
     * <p>
     * The 'id' and 'status' are ignored to allow the database (UUID generation) 
     * and service layer (lifecycle logic) to manage these fields exclusively.
     * </p>
     * @param dto the source data transfer object.
     * @return the mapped Program entity.
     */
    @Mapping(target = "programId", ignore = true)
    @Mapping(target = "status", ignore = true)
    Program toEntity(ProgramRequestDTO dto);

    /**
     * Converts a Program entity into a response DTO for the API layer.
     * <p>
     * Includes the mapping of nested collections (rules and documents) 
     * along with their respective database-generated UUIDs.
     * </p>
     * @param program the source entity from the database.
     * @return the response DTO containing program details and nested collections.
     */
    @Mapping(source = "programId", target = "programId")
    ProgramResponseDTO toResponseDTO(Program program);
    /**
     * Maps an EligibilityRuleDTO to its corresponding entity.
     * <p>
     * The 'ruleId' is NOT ignored here. This allows the manual merge logic in the 
     * service layer to identify if an incoming rule is an update to an existing record 
     * or a new entry. The 'program' reference is ignored to prevent circular dependencies 
     * during mapping and is set manually in the service.
     * </p>
     * @param dto the rule data transfer object.
     * @return the mapped EligibilityRule entity.
     */
    @Mapping(target = "program", ignore = true)
    EligibilityRule toRuleEntity(EligibilityRuleDTO dto);

    /**
     * Maps a RequiredDocumentDTO to its corresponding entity.
     * <p>
     * The 'documentId' is mapped to ensure existing documents can be 
     * identified during partial updates. Similar to rules, the 'program' 
     * reference is ignored and must be linked manually.
     * </p>
     * @param dto the document data transfer object.
     * @return the mapped RequiredDocument entity.
     */
    @Mapping(target = "program", ignore = true)
    RequiredDocument toDocumentEntity(RequiredDocumentDTO dto);

    /**
     * Updates an existing Program entity with data from a request DTO.
     * <p>
     * Used for partial updates to top-level program fields like name, budget, or dates.
     * Primary key 'id' and lifecycle 'status' are protected from modification.
     * </p>
     * @param dto the source DTO containing updated values.
     * @param target the existing entity to be modified.
     */
    @Mapping(target = "programId", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDto(ProgramRequestDTO dto, @MappingTarget Program target);
}

