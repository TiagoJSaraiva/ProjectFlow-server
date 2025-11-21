package com.polo42.project_flow.controller;

import com.polo42.project_flow.dto.ElementDTO;
import com.polo42.project_flow.service.ElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/elements")
public class ElementController {

    @Autowired
    private ElementService elementService;

    // 🔹 Listar todos os elementos de um projeto
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ElementDTO>> getElementsByProject(@PathVariable UUID projectId) {
        List<ElementDTO> elements = elementService.findByProjetoId(projectId);
        return ResponseEntity.ok(elements);
    }

    // 🔹 Obter um elemento pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<ElementDTO> getElementById(@PathVariable UUID id) {
        ElementDTO element = elementService.findById(id);
        return ResponseEntity.ok(element);
    }

    // 🔹 Criar um novo elemento vinculado a um projeto
    @PostMapping("/project/{projectId}")
    public ResponseEntity<ElementDTO> createElement(@PathVariable UUID projectId, @RequestBody ElementDTO elementDTO) {
        ElementDTO savedElement = elementService.create(projectId, elementDTO);
        return ResponseEntity.ok(savedElement);
    }

    // 🔹 Atualizar dados básicos do elemento (nome, descrição)
    @PutMapping("/{id}")
    public ResponseEntity<ElementDTO> updateElement(@PathVariable UUID id, @RequestBody ElementDTO elementDTO) {
        ElementDTO updatedElement = elementService.update(id, elementDTO);
        return ResponseEntity.ok(updatedElement);
    }

    // 🔹 Atualizar conteúdo do elemento (texto interno)
    @PutMapping("/{id}/content")
    public ResponseEntity<ElementDTO> updateElementContent(@PathVariable UUID id, @RequestBody String conteudo) {
        ElementDTO updatedElement = elementService.updateContent(id, conteudo);
        return ResponseEntity.ok(updatedElement);
    }

    // 🔹 Atualizar coordenadas do elemento no canvas
    @PutMapping("/{id}/coordinates")
    public ResponseEntity<ElementDTO> updateElementCoordinates(@PathVariable UUID id, @RequestBody String coordenadas) {
        ElementDTO updatedElement = elementService.updateCoordinates(id, coordenadas);
        return ResponseEntity.ok(updatedElement);
    }

    // 🔹 Atualizar relações/conexões do elemento
    @PutMapping("/{id}/relations")
    public ResponseEntity<ElementDTO> updateElementRelations(@PathVariable UUID id, @RequestBody String relacoes) {
        ElementDTO updatedElement = elementService.updateRelations(id, relacoes);
        return ResponseEntity.ok(updatedElement);
    }

    // 🔹 Deletar um elemento
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteElement(@PathVariable UUID id) {
        elementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}