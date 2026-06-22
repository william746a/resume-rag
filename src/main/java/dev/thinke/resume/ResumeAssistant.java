package dev.thinke.resume;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.smallrye.mutiny.Multi;

@RegisterAiService(retrievalAugmentor = ResumeRetrievalAugmentor.class)
@SystemMessage(fromResource = "prompts/system.txt")
public interface ResumeAssistant {

    @UserMessage("Question: {{question}}")
    Multi<String> ask(@V("question") String question);
}
