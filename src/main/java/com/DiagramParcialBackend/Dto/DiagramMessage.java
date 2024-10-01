package com.DiagramParcialBackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiagramMessage {
    private Long sessionId;
    private Map<String, Object> data;
}