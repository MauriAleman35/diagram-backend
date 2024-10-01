package com.DiagramParcialBackend.Controllers;

import com.DiagramParcialBackend.Dto.LockDto;
import com.DiagramParcialBackend.Entity.Lock;
import com.DiagramParcialBackend.Services.LockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locks")
public class LockController {

    @Autowired
    private LockService lockService;

    // Crear un bloqueo para una entidad específica
    @PostMapping("/create")
    public ResponseEntity<Lock> createLock(@Valid @RequestBody LockDto lockDto) {
        Lock lock = lockService.createLock(lockDto);
        return ResponseEntity.ok(lock);
    }

    // Liberar un bloqueo en una entidad específica
    @DeleteMapping("/release")
    public ResponseEntity<String> releaseLock(@Valid @RequestBody LockDto lockDto) {
        lockService.releaseLock(lockDto);
        return ResponseEntity.ok("Bloqueo liberado con éxito");
    }

    // Verificar si una entidad está bloqueada
    @GetMapping("/isLocked")
    public ResponseEntity<Boolean> isLocked(@RequestParam Long diagramId, @RequestParam Long entityKey) {
        boolean locked = lockService.isLocked(diagramId, entityKey);
        return ResponseEntity.ok(locked);
    }
}
