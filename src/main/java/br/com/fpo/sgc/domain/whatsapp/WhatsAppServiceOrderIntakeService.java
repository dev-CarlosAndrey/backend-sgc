package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderChannel;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPriority;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderService;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre uma OS a partir de um {@link WhatsAppServiceOrderCommand} já interpretado (RF15). Decisões de
 * produto: qualquer pessoa pode abrir chamado (um {@link User} é criado na hora se o telefone não existir
 * ainda, via {@link UserService#findOrCreateByPhone}); setor e bem são casados por nome/tombamento exatos
 * (sem busca aproximada, para manter o fluxo em uma única mensagem); prioridade não é perguntada — toda OS
 * aberta pelo WhatsApp entra como MEDIUM.
 */
@Service
@RequiredArgsConstructor
class WhatsAppServiceOrderIntakeService {

    private final SectorRepository sectorRepository;
    private final AssetRepository assetRepository;
    private final UserService userService;
    private final ServiceOrderService serviceOrderService;

    @Transactional
    IntakeResult open(WhatsAppServiceOrderCommand command, String fromPhone, String displayName) {
        Sector sector = sectorRepository.findByNameIgnoreCaseAndActiveTrue(command.sectorName()).orElse(null);
        if (sector == null) {
            return IntakeResult.sectorNotFound(command.sectorName());
        }

        Asset asset = null;
        if (command.assetTagNumber() != null) {
            asset = assetRepository.findByTagNumberIgnoreCaseAndActiveTrue(command.assetTagNumber()).orElse(null);
            if (asset == null) {
                return IntakeResult.assetNotFound(command.assetTagNumber());
            }
        }

        User requester = userService.findOrCreateByPhone(fromPhone, displayName);

        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO(
                buildTitle(command.problemDescription()),
                command.problemDescription(),
                ServiceOrderPriority.MEDIUM,
                requester.getId(),
                asset != null ? asset.getId() : null,
                sector.getId());

        ServiceOrderResponseDTO created = serviceOrderService.create(dto, ServiceOrderChannel.WHATSAPP);
        return IntakeResult.success(created);
    }

    // Título obrigatório (5-150 caracteres) que o formato do WhatsApp não pede separadamente.
    private String buildTitle(String problemDescription) {
        String firstLine = problemDescription.lines().findFirst().orElse(problemDescription).trim();
        String title = "Chamado via WhatsApp: " + firstLine;
        return title.length() > 150 ? title.substring(0, 150) : title;
    }

    sealed interface IntakeResult {
        record Success(ServiceOrderResponseDTO serviceOrder) implements IntakeResult {}
        record SectorNotFound(String sectorName) implements IntakeResult {}
        record AssetNotFound(String tagNumber) implements IntakeResult {}

        static IntakeResult success(ServiceOrderResponseDTO so) {
            return new Success(so);
        }

        static IntakeResult sectorNotFound(String sectorName) {
            return new SectorNotFound(sectorName);
        }

        static IntakeResult assetNotFound(String tagNumber) {
            return new AssetNotFound(tagNumber);
        }
    }
}
