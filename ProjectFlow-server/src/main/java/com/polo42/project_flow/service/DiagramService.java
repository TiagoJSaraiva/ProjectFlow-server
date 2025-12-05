package com.polo42.project_flow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo42.project_flow.dto.DiagramConnectionDTO;
import com.polo42.project_flow.dto.DiagramNodeDTO;
import com.polo42.project_flow.dto.DiagramPointDTO;
import com.polo42.project_flow.dto.DiagramViewportDTO;
import com.polo42.project_flow.dto.ProjectDiagramDTO;
import com.polo42.project_flow.dto.ProjectDiagramMetadataDTO;
import com.polo42.project_flow.dto.SaveProjectDiagramRequest;
import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.exception.ResourceNotFoundException;
import com.polo42.project_flow.model.DiagramConnection;
import com.polo42.project_flow.model.DiagramNode;
import com.polo42.project_flow.model.DiagramNodeType;
import com.polo42.project_flow.model.DiagramViewState;
import com.polo42.project_flow.model.Project;
import com.polo42.project_flow.repository.DiagramConnectionRepository;
import com.polo42.project_flow.repository.DiagramNodeRepository;
import com.polo42.project_flow.repository.DiagramViewStateRepository;
import com.polo42.project_flow.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiagramService {

    private static final Logger log = LoggerFactory.getLogger(DiagramService.class);

    private final ProjectRepository projectRepository;
    private final DiagramNodeRepository nodeRepository;
    private final DiagramConnectionRepository connectionRepository;
    private final DiagramViewStateRepository viewStateRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DiagramService(
            ProjectRepository projectRepository,
            DiagramNodeRepository nodeRepository,
            DiagramConnectionRepository connectionRepository,
            DiagramViewStateRepository viewStateRepository,
            ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.connectionRepository = connectionRepository;
        this.viewStateRepository = viewStateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProjectDiagramDTO saveDiagram(UUID projectId, SaveProjectDiagramRequest request) {
        log.debug("[DiagramService] Persisting diagram for project {}", projectId);
        try {
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", projectId.toString()));

            applyProjectMetadata(project, request.getProject());
            project.setUltimaAlteracao(LocalDateTime.now());
            projectRepository.save(project);

            connectionRepository.deleteByProjetoId(projectId);
            nodeRepository.deleteByProjetoId(projectId);

            List<DiagramNode> nodes = storeNodes(project, request.getNodes());
            Map<UUID, DiagramNode> nodeIndex = nodes.stream()
                .collect(Collectors.toMap(DiagramNode::getId, node -> node));

            List<DiagramConnection> connections = storeConnections(project, request.getConnections(), nodeIndex);
            log.debug("[DiagramService] Prepared {} nodes and {} connections for project {}", nodes.size(), connections.size(), projectId);

            DiagramViewState viewState = viewStateRepository.findByProjetoId(projectId)
                .orElseGet(() -> {
                DiagramViewState state = new DiagramViewState();
                state.setProjeto(project);
                return state;
                });
            applyViewport(viewState, request.getViewport());
            viewState.setLastSavedAt(LocalDateTime.now());
            viewStateRepository.save(viewState);

            nodeRepository.saveAll(nodes);
            connectionRepository.saveAll(connections);

            return loadDiagram(projectId);
        } catch (RuntimeException exception) {
            log.error("[DiagramService] Failed to persist diagram for project {}", projectId, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ProjectDiagramDTO loadDiagram(UUID projectId) {
        log.debug("[DiagramService] Loading diagram for project {}", projectId);
        try {
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", "id", projectId.toString()));

            List<DiagramNodeDTO> nodeDTOs = nodeRepository.findByProjetoIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(this::toNodeDTO)
                .collect(Collectors.toList());

            List<DiagramConnectionDTO> connectionDTOs = connectionRepository.findByProjetoId(projectId)
                .stream()
                .map(this::toConnectionDTO)
                .collect(Collectors.toList());

            log.debug("[DiagramService] Found {} nodes and {} connections for project {}", nodeDTOs.size(), connectionDTOs.size(), projectId);

            DiagramViewportDTO viewport = viewStateRepository.findByProjetoId(projectId)
                .map(this::toViewportDTO)
                .orElseGet(() -> new DiagramViewportDTO(1, 0, 0, 32));

            ProjectDiagramDTO response = new ProjectDiagramDTO();
            response.setProjectId(project.getId());
            response.setProjectName(project.getNome());
            response.setProjectDescription(project.getDescricao());
            response.setViewport(viewport);
            response.setNodes(nodeDTOs);
            response.setConnections(connectionDTOs);
            return response;
        } catch (RuntimeException exception) {
            log.error("[DiagramService] Failed to load diagram for project {}", projectId, exception);
            throw exception;
        }
    }

    private void applyProjectMetadata(Project project, ProjectDiagramMetadataDTO metadata) {
        if (metadata == null) {
            return;
        }

        if (metadata.getName() != null && !metadata.getName().isBlank()) {
            if (metadata.getName().length() > 100) {
                throw new BusinessException("O nome do projeto não pode ultrapassar 100 caracteres.");
            }
            project.setNome(metadata.getName().trim());
        }

        if (metadata.getDescription() != null) {
            if (metadata.getDescription().length() > 10_000) {
                throw new BusinessException("A descrição do projeto não pode ultrapassar 10.000 caracteres.");
            }
            project.setDescricao(metadata.getDescription());
        }
    }

    private List<DiagramNode> storeNodes(Project project, List<DiagramNodeDTO> nodes) {
        List<DiagramNodeDTO> safeNodes = nodes != null ? nodes : new ArrayList<>();
        List<DiagramNode> entities = new ArrayList<>(safeNodes.size());

        for (DiagramNodeDTO dto : safeNodes) {
            validateNode(dto);
            DiagramNode entity = new DiagramNode();
            entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
            entity.setProjeto(project);
            entity.setTipo(dto.getType() != null ? dto.getType() : DiagramNodeType.RECTANGLE);
            entity.setNome(dto.getName() != null && !dto.getName().isBlank() ? dto.getName().trim() : "Sample text");
            entity.setDescricao(dto.getDescription());
            entity.setPosX(dto.getX());
            entity.setPosY(dto.getY());
            entity.setLargura(dto.getWidth());
            entity.setAltura(dto.getHeight());
            entity.setRaio(dto.getRadius());
            entity.setRotacao(dto.getRotation());
            entity.setTexto(writeJson(dto.getText()));
            entity.setMetadata(writeJson(dto.getMetadata()));
            entities.add(entity);
        }

        return entities;
    }

    private List<DiagramConnection> storeConnections(Project project,
                                                     List<DiagramConnectionDTO> connections,
                                                     Map<UUID, DiagramNode> nodeIndex) {
        List<DiagramConnectionDTO> safeConnections = connections != null ? connections : new ArrayList<>();
        List<DiagramConnection> entities = new ArrayList<>(safeConnections.size());

        for (DiagramConnectionDTO dto : safeConnections) {
            if (dto.getFromNodeId() == null || dto.getToNodeId() == null) {
                throw new BusinessException("Conexões precisam indicar nós de origem e destino.");
            }
            DiagramNode from = nodeIndex.get(dto.getFromNodeId());
            DiagramNode to = nodeIndex.get(dto.getToNodeId());
            if (from == null || to == null) {
                throw new BusinessException("Uma conexão faz referência a nós inexistentes.");
            }

            DiagramConnection entity = new DiagramConnection();
            entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
            entity.setProjeto(project);
            entity.setFromNode(from);
            entity.setToNode(to);
            entity.setBendPoint(writePoint(dto.getBendPoint()));
            entity.setEstilo(writeJson(dto.getStyle()));
            entity.setMetadata(writeJson(dto.getMetadata()));
            entities.add(entity);
        }

        return entities;
    }

    private void validateNode(DiagramNodeDTO dto) {
        String name = dto.getName();
        if (name != null && name.length() > 20) {
            throw new BusinessException("O nome do elemento não pode ultrapassar 20 caracteres.");
        }
        String description = dto.getDescription();
        if (description != null && description.length() > 10_000) {
            throw new BusinessException("A descrição do elemento não pode ultrapassar 10.000 caracteres.");
        }
    }

    private DiagramNodeDTO toNodeDTO(DiagramNode entity) {
        DiagramNodeDTO dto = new DiagramNodeDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getTipo());
        dto.setName(entity.getNome());
        dto.setDescription(entity.getDescricao());
        dto.setX(entity.getPosX());
        dto.setY(entity.getPosY());
        dto.setWidth(entity.getLargura());
        dto.setHeight(entity.getAltura());
        dto.setRadius(entity.getRaio());
        dto.setRotation(entity.getRotacao());
        dto.setText(readJson(entity.getTexto()));
        dto.setMetadata(readJson(entity.getMetadata()));
        return dto;
    }

    private DiagramConnectionDTO toConnectionDTO(DiagramConnection entity) {
        DiagramConnectionDTO dto = new DiagramConnectionDTO();
        dto.setId(entity.getId());
        dto.setFromNodeId(entity.getFromNode().getId());
        dto.setToNodeId(entity.getToNode().getId());
        dto.setBendPoint(readPoint(entity.getBendPoint()));
        dto.setStyle(readJson(entity.getEstilo()));
        dto.setMetadata(readJson(entity.getMetadata()));
        return dto;
    }

    private DiagramViewportDTO toViewportDTO(DiagramViewState state) {
        return new DiagramViewportDTO(state.getZoom(), state.getOffsetX(), state.getOffsetY(), state.getGridSize());
    }

    private void applyViewport(DiagramViewState state, DiagramViewportDTO viewport) {
        if (viewport == null) {
            return;
        }
        state.setZoom(viewport.getZoom());
        state.setOffsetX(viewport.getOffsetX());
        state.setOffsetY(viewport.getOffsetY());
        state.setGridSize(viewport.getGridSize());
    }

    private Map<String, Object> readJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private DiagramPointDTO readPoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, DiagramPointDTO.class);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String writeJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Não foi possível serializar os dados do elemento.");
        }
    }

    private String writePoint(DiagramPointDTO point) {
        if (point == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(point);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Não foi possível serializar os pontos da conexão.");
        }
    }
}
