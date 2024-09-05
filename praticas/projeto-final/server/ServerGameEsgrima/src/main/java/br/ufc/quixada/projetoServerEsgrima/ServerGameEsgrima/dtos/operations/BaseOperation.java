package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.UserToken;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.request.LoginOperationRequest;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response.ConnectionOperationResponse;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response.RankingResponse;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.EncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class BaseOperation {
    private String operation;
    private String data;
    private Boolean isEncrypted;


    public BaseOperation(String operation, String jsonString, Boolean isEncrypted) {
        this.operation = operation;
        this.data = jsonString;
        this.isEncrypted = isEncrypted;
    }

    public BaseOperation(String operation, JsonNode jsonNode, Boolean isEncrypted) {
        this.operation = operation;
        this.data = jsonNode.asText() == null? null : jsonNode.asText();
        this.isEncrypted = isEncrypted;
    }

    public BaseOperation(String jsonString) {
        try {
            JsonNode node = new ObjectMapper().readTree(jsonString);
            this.operation = node.get("operation").asText();
            this.data = node.get("data").asText().isEmpty()? null : node.get("data").asText();
            this.isEncrypted = node.get("isEncrypted").asBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            this.operation = null;
            this.data = null;
            this.isEncrypted = null;
        }
    }

    public BaseOperation(JsonNode jsonNode) {
        this.operation = jsonNode.get("operation").asText();
        this.data = jsonNode.get("data").asText().isEmpty()? null : jsonNode.get("data").asText();
        this.isEncrypted = jsonNode.get("isEncrypted").asBoolean();
    }

    public String stringify() {
        try {
            return new ObjectMapper().writeValueAsString(new BaseOperationRecord(this.operation, this.data, this.isEncrypted));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BaseOperation readBase64(String base64) {
        try {
            return new BaseOperation(new ObjectMapper().readTree(new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] stringifyToBase64() {
        try {
            return Base64.getEncoder().encode(this.stringify().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JsonNode nodefy() {
        try {
            return new ObjectMapper().readTree(this.stringify());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static BaseOperation parseJsonToOperation(String json) {
        try {
            JsonNode node = new ObjectMapper().readTree(json);
            return new BaseOperation(node.get("operation").asText(), node.get("data").asText(), node.get("isEncrypted").asBoolean());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public JsonNode getDataAsJsonNode() {
        ObjectMapper mapper = new ObjectMapper();
        if (this.isEncrypted || this.data == null) {
            return null;
        }
        try {
            return mapper.readTree(this.data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String getDataAsString() {
        try {
            if (Objects.isNull(this.data)) {
                return null;
            }
            return new ObjectMapper().writeValueAsString(this.data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public Object getDataAsObject() {
        if (this.data == null || this.isEncrypted) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            switch (this.operation) {
                case "msg", "error", "login_fail", "register_fail":
                    return mapper.readValue(this.data, MessageOperation.class);
                case "login", "register":
                    return mapper.readValue(this.data, LoginOperationRequest.class);
                case "login_success", "register_success":
                    return mapper.readValue(this.data, UserToken.class);
                case "establish_connection":
                    return mapper.readValue(this.data, ConnectionOperationResponse.class);
                case "ranking":
                    return mapper.readValue(this.data, RankingResponse.class);
                // Casos que entram no default:
                // "ok", "connect", "logout", "get_ranking", "logout_success", "connection_success", "connection_fail", "logout_fail":
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public void encryptData() {
        if (this.isEncrypted || this.data == null) {
            return;
        }
        EncryptionService encryptionService = new EncryptionService();
        this.data = encryptionService.encrypt(this.data);
        this.isEncrypted = true;
    }


    public void decryptData() {
        if (!this.isEncrypted || this.data == null) {
            return;
        }
        EncryptionService encryptionService = new EncryptionService();
        this.data = encryptionService.decrypt(this.data);
        this.isEncrypted = false;
    }


}