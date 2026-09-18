package com.academic.planning.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface LlmClient {
    JsonNode complete(List<JsonNode> messages, JsonNode tools);
}
