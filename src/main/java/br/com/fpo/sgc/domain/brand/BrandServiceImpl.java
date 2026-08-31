package br.com.fpo.sgc.domain.brand;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.brand.dto.BrandRequestDTO;
import br.com.fpo.sgc.domain.brand.dto.BrandResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public BrandResponseDTO create(BrandRequestDTO dto) {
        if (brandRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Já existe uma marca cadastrada com o nome: " + dto.name());
        }

        Brand brand = Brand.builder()
                .name(dto.name())
                .active(true)
                .build();

        Brand savedBrand = brandRepository.save(brand);

        auditLogService.log(
                "Brand",
                savedBrand.getId(),
                "CREATE",
                savedBrand.getName(),
                "Marca " + savedBrand.getName() + " cadastrada"
        );

        return BrandResponseDTO.fromEntity(savedBrand);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponseDTO findById(Long id) {
        Brand brand = brandRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Marca não encontrada com o ID: " + id));
        return BrandResponseDTO.fromEntity(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponseDTO> findAll(Pageable pageable) {
        return brandRepository.findByActiveTrue(pageable)
                .map(BrandResponseDTO::fromEntity);
    }

    @Override
    @Transactional
    public BrandResponseDTO update(Long id, BrandRequestDTO dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Marca não encontrada com o ID: " + id));

        if (!brand.getName().equalsIgnoreCase(dto.name()) && brandRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Já existe outra marca cadastrada com o nome: " + dto.name());
        }

        brand.setName(dto.name());
        Brand updatedBrand = brandRepository.save(brand);

        auditLogService.log(
                "Brand",
                updatedBrand.getId(),
                "UPDATE",
                updatedBrand.getName(),
                "Marca renomeada para " + updatedBrand.getName()
        );

        return BrandResponseDTO.fromEntity(updatedBrand);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Marca não encontrada com o ID: " + id));

        brand.setActive(false);
        brandRepository.save(brand);

        auditLogService.log(
                "Brand",
                id,
                "DELETE",
                brand.getName(),
                "Marca " + brand.getName() + " inativada"
        );
    }
}