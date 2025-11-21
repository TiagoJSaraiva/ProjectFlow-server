package com.polo42.project_flow.service;

import com.polo42.project_flow.dto.ElementDTO;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.model.Element;
import com.polo42.project_flow.model.ElementType;
import com.polo42.project_flow.model.Project;
import com.polo42.project_flow.repository.ElementRepository;
import com.polo42.project_flow.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ElementService {

    @Autowired
    private ElementRepository elementRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // 🔹 Buscar elemento por ID
    public ElementDTO findById(UUID id) {
        Element element = elementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elemento", "id", id.toString()));
        return convertToDTO(element);
    }

    // 🔹 Listar elementos de um projeto
    public List<ElementDTO> findByProjetoId(UUID projetoId) {
        if (!projectRepository.existsById(projetoId)) {
            throw new ResourceNotFoundException("Projeto", "id", projetoId.toString());
        }

        return elementRepository.findByProjetoId(projetoId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Criar novo elemento
    public ElementDTO create(UUID projetoId, ElementDTO elementDTO) {
        Project projeto = projectRepository.findById(projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", projetoId.toString()));

        Element element = new Element();
        element.setNome(elementDTO.getNome());
        element.setDescricao(elementDTO.getDescricao());
        element.setConteudo(elementDTO.getConteudo());
        element.setTipo(elementDTO.getTipo() != null ? elementDTO.getTipo() : ElementType.TEXTO);
        element.setCoordenadas(elementDTO.getCoordenadas());
        element.setRelacoes(elementDTO.getRelacoes());
        element.setProjeto(projeto);
        element.setDataCriado(LocalDateTime.now());
        element.setUltimaAlteracao(LocalDateTime.now());

        Element savedElement = elementRepository.save(element);
        return convertToDTO(savedElement);
    }

    // 🔹 Atualizar dados básicos do elemento
    public ElementDTO update(UUID id, ElementDTO elementDTO) {
        Element element = elementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elemento", "id", id.toString()));

        element.setNome(elementDTO.getNome());
        element.setDescricao(elementDTO.getDescricao());
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Atualizar conteúdo do elemento
    public ElementDTO updateContent(UUID id, String conteudo) {
        Element element = elementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elemento", "id", id.toString()));

        element.setConteudo(conteudo);
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Atualizar coordenadas do elemento
    public ElementDTO updateCoordinates(UUID id, String coordenadas) {
        Element element = elementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elemento", "id", id.toString()));

        element.setCoordenadas(coordenadas);
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Atualizar relações do elemento
    public ElementDTO updateRelations(UUID id, String relacoes) {
        Element element = elementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elemento", "id", id.toString()));

        element.setRelacoes(relacoes);
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Deletar elemento
    public void delete(UUID id) {
        if (!elementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Elemento", "id", id.toString());
        }
        elementRepository.deleteById(id);
    }

    // 🔹 Converter Element para ElementDTO
    private ElementDTO convertToDTO(Element element) {
        return new ElementDTO(
            element.getId(),
            element.getNome(),
            element.getDescricao(),
            element.getConteudo(),
            element.getTipo(),
            element.getCoordenadas(),
            element.getRelacoes(),
            element.getDataCriado(),
            element.getUltimaAlteracao(),
            element.getProjeto().getId()
        );
    }
}