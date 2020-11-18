package living.tanach.api.model.entities;

import dev.sanda.apifi.annotations.EntityCollectionApi;
import living.tanach.api.api_hooks.LinkedMediaContentOfTagApiHooks;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Fetch;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static dev.sanda.apifi.generator.entity.EntityCollectionEndpointType.*;
import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.FetchMode.JOIN;

@Data
@Entity
@ToString(exclude = {"verses", "linkedContent"})
@EqualsAndHashCode(exclude = {"verses", "linkedContent"})
public class MediaTag {
    @Id
    @GeneratedValue
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String key;
    @Column(columnDefinition = "TEXT")
    private String description;
    @ManyToMany
    private Set<Verse> verses = new HashSet<>();
    @Fetch(JOIN)
    @OneToMany(cascade = ALL)
    @EntityCollectionApi(
            endpoints = {ASSOCIATE_WITH, UPDATE_IN, REMOVE_FROM},
            apiHooks = LinkedMediaContentOfTagApiHooks.class
            )
    private Set<MediaContent> linkedContent = new HashSet<>();
}