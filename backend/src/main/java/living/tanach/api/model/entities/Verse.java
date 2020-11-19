package living.tanach.api.model.entities;

import dev.sanda.apifi.annotations.ApiFindByUnique;
import dev.sanda.apifi.annotations.EntityCollectionApi;
import dev.sanda.apifi.annotations.WithApiFreeTextSearchByFields;
import io.leangen.graphql.annotations.GraphQLQuery;
import living.tanach.api.api_hooks.MediaTagsOfVerseApiHooks;
import living.tanach.api.model.transients.HighlightedVerseSegments;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static dev.sanda.apifi.generator.entity.EntityCollectionEndpointType.*;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;
import static living.tanach.api.utils.StaticUtils.toHebrewNumeral;
import static living.tanach.api.utils.StaticUtils.toHumanReadableHebrewPath;
import static org.hibernate.annotations.FetchMode.JOIN;

@Data
@Entity
@Indexed
@ToString(exclude = "chapter")
@EqualsAndHashCode(exclude = "chapter")
@Table(indexes = {@Index(name = "path", columnList = "path")})
@WithApiFreeTextSearchByFields("searchableHebrewText")
public class Verse {
    @Id
    @GeneratedValue
    @SortableField
    private Long id;
    @Field
    @SortableField
    @ApiFindByUnique
    @Column(unique = true)
    private String path;
    @SortableField
    @Field
    private Integer number;
    @Fetch(JOIN)
    @ManyToOne(fetch = LAZY, optional = false)
    private Chapter chapter;
    @Fetch(JOIN)
    @ManyToMany(cascade = ALL, fetch = LAZY)
    @EntityCollectionApi(
            endpoints = {ASSOCIATE_WITH, UPDATE_IN, REMOVE_FROM},
            apiHooks = MediaTagsOfVerseApiHooks.class
    )
    private Set<MediaTag> mediaTags = new HashSet<>();
    @Column(columnDefinition = "TEXT")
    private String fullHebrewText;
    @Column(columnDefinition = "TEXT")
    private String fullEnglishText;
    @Field
    @Column(columnDefinition = "TEXT")
    private String searchableHebrewText;

    public String getHebrewNumeral(){
        return toHebrewNumeral(this.number);
    }

    public String getHumanReadablePath(){
        return toHumanReadableHebrewPath(path);
    }

    @Transient
    @Getter(onMethod_ = @GraphQLQuery)
    private HighlightedVerseSegments highlightedVerseSegments;
}
