package com.DiagramParcialBackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockMessage {
    private Long userId;
    private Long entityKey;
    private String status;  // "locked" o "unlocked"
}