package ar.edu.utn.frc.tup.piii.models;

// representa el "stage" de un Pokémon: Básico, Etapa 1, Etapa 2, Mega.
// Se deriva de los subtypes en el JSON.
public enum PokemonStage {
    BASIC("Basic"),
    STAGE_1("Stage 1"),
    STAGE_2("Stage 2"),
    MEGA("Mega");
    
    private final String jsonValue;

    PokemonStage(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String getJsonValue() {
        return jsonValue;
    }

    // Método para convertir del string del JSON al enum
    public static PokemonStage fromJson(String value) {
        for (PokemonStage stage : values()) {
            if (stage.jsonValue.equals(value)) return stage;
        }
        throw new IllegalArgumentException("Stage desconocido: " + value);
    }
}
