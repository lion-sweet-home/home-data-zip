package org.example.homedatazip.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.homedatazip.common.BaseTimeEntity;

@Entity
@Getter
@Setter
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String message;

    // TODO: type 추가하면 좋을듯
}
