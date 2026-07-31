package ar.edu.utn.frc.tup.piii.dtos.skin;

import java.util.List;

import lombok.Data;

/**
 * DTO para deserializar skin-mapping.json.
 */
@Data
public class SkinMappingDTO {

    private List<OfficialSkinEntry> official;
    private List<AlternativeSkinEntry> alternative;

    /**
     * Entrada de skin oficial (full art del TCG).
     */
    @Data
    public static class OfficialSkinEntry {
        private String skinId;
        private String name;
        private String baseCardId;
        private String imageUrl;
        private String imageUrlLarge;
        private String setCode;
        private String tag;
    }

    /**
     * Entrada de skin alternativa (arte custom en Minio).
     */
    @Data
    public static class AlternativeSkinEntry {
        private String skinId;
        private String name;
        private String filename;
        private String baseCardId;
        private String tag;
    }
}
