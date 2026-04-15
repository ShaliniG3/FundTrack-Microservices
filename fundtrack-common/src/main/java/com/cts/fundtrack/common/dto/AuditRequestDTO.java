package com.cts.fundtrack.common.dto;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequestDTO {
    private UUID userId;       
    private ActionType action;  
    private UUID entityId;     
    private EntityType entityName; 
}