package br.com.fpo.sgc.common.storage;

import br.com.fpo.sgc.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String subDirectory, Set<String> allowedContentTypes, long maxSizeBytes) {
        validate(file, allowedContentTypes, maxSizeBytes);

        try {
            String fileName = UUID.randomUUID() + extractExtension(file.getOriginalFilename());

            Path targetDir = Path.of(uploadDir, subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(fileName);
            file.transferTo(targetFile);

            return "/uploads/" + subDirectory + "/" + fileName;
        } catch (IOException e) {
            throw new BusinessException("Falha ao salvar o arquivo enviado.", e);
        }
    }

    private void validate(MultipartFile file, Set<String> allowedContentTypes, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Nenhum arquivo foi enviado.");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("O arquivo excede o tamanho máximo permitido de " + (maxSizeBytes / (1024 * 1024)) + "MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException("Tipo de arquivo não suportado. Tipos permitidos: " + allowedContentTypes);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
    }
}
