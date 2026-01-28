package org.example.homedatazip.test.ai.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.example.homedatazip.user.entity.User;

@Entity
@Data
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String role;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String sessionId;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public static AiMessage createMessage(User user, String sessionId, String content, String role) {
        AiMessage aiMessage = new AiMessage();
        aiMessage.user = user;
        aiMessage.sessionId = sessionId;
        aiMessage.content = content;
        aiMessage.role = role;
        return aiMessage;
    }


}
