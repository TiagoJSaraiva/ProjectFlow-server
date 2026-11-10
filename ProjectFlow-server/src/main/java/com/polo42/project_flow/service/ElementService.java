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
    public ElementDTO findById(UUID id, UUID currentUserId) {
        Element element = findOwned(id, currentUserId);
        return convertToDTO(element);
    }

    // 🔹 Listar elementos de um projeto
    public List<ElementDTO> findByProjetoId(UUID projetoId, UUID currentUserId) {
        findOwnedProject(projetoId, currentUserId);

        return elementRepository.findByProjetoId(projetoId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Criar novo elemento
    public ElementDTO create(UUID projetoId, UUID currentUserId, ElementDTO elementDTO) {
        Project projeto = findOwnedProject(projetoId, currentUserId);

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
    public ElementDTO update(UUID id, UUID currentUserId, ElementDTO elementDTO) {
        Element element = findOwned(id, currentUserId);

        element.setNome(elementDTO.getNome());
        element.setDescricao(elementDTO.getDescricao());
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Atualizar conteúdo do elemento
    public ElementDTO updateContent(UUID id, UUID currentUserId, String conteudo) {
        Element element = findOwned(id, currentUserId);

        element.setConteudo(conteudo);
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Atualizar coordenadas do elemento
    public ElementDTO updateCoordinates(UUID id, UUID currentUserId, String coordenadas) {
        Element element = findOwned(id, currentUserId);

        element.setCoordenadas(coordenadas);
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Atualizar relações do elemento
    public ElementDTO updateRelations(UUID id, UUID currentUserId, String relacoes) {
        Element element = findOwned(id, currentUserId);

        element.setRelacoes(relacoes);
        element.setUltimaAlteracao(LocalDateTime.now());

        Element updatedElement = elementRepository.save(element);
        return convertToDTO(updatedElement);
    }

    // 🔹 Deletar elemento
    public void delete(UUID id, UUID currentUserId) {
        Element element = findOwned(id, currentUserId);
        elementRepository.delete(element);
    }

    // Projeto/elemento de outro usuário é tratado como inexistente (404)
    private Project findOwnedProject(UUID projetoId, UUID currentUserId) {
        return projectRepository.findByIdAndUsuarioId(projetoId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", projetoId.toString()));
    }

    private Element findOwned(UUID id, UUID currentUserId) {
        return elementRepository.findById(id)
                .filter(e -> e.getProjeto().getUsuario().getId().equals(currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Elemento", "id", id.toString()));
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