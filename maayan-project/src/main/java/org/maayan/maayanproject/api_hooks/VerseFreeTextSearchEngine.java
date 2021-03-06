package org.maayan.maayanproject.api_hooks;

import dev.sanda.apifi.service.api_hooks.ApiHooks;
import dev.sanda.datafi.dto.FreeTextSearchPageRequest;
import dev.sanda.datafi.dto.Page;
import dev.sanda.datafi.service.DataManager;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import lombok.val;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.MustJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.maayan.maayanproject.model.entities.Verse;
import org.maayan.maayanproject.model.transients.HighlightedVerseSegments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerseFreeTextSearchEngine implements ApiHooks<Verse> {

  @Override
  @SuppressWarnings("unchecked")
  public Page<Verse> executeCustomFreeTextSearch(
    FreeTextSearchPageRequest request,
    DataManager<Verse> dataManager
  ) {
    val queryBuilder = queryBuilder(dataManager);
    val query = query(request, queryBuilder);
    val sort = sort(request, queryBuilder);
    val fullTextEntityManager = Search.getFullTextEntityManager(
      dataManager.entityManager()
    );
    val fullTextQuery = fullTextQuery(
      query,
      sort,
      fullTextEntityManager,
      request
    );
    val results = fullTextQuery.getResultList();
    val totalHits = fullTextQuery.getResultSize();
    val response = new Page<Verse>();
    response.setContent(results);
    response.setPageNumber(request.getPageNumber());
    response.setTotalItemsCount((long) totalHits);
    response.setTotalPagesCount(
      (long) Math.ceil((double) totalHits / request.getPageSize())
    );
    setHighlightedVerseSegments(results, request.getSearchTerm());
    return response;
  }

  private void setHighlightedVerseSegments(
    List<Verse> results,
    String searchTerm
  ) {
    results.forEach(
      verse ->
        verse.setHighlightedVerseSegments(
          new HighlightedVerseSegments(verse, searchTerm)
        )
    );
  }

  private FullTextQuery fullTextQuery(
    Query query,
    Sort sort,
    FullTextEntityManager textEntityManager,
    FreeTextSearchPageRequest request
  ) {
    val fullTextQuery = textEntityManager
      .createFullTextQuery(query, Verse.class)
      .setSort(sort);
    if (!request.getFetchAll()) {
      var offset = request.getPageNumber() * request.getPageSize();
      var limit = request.getPageSize();
      fullTextQuery.setFirstResult(offset).setMaxResults(limit);
    }
    return fullTextQuery;
  }

  private Sort sort(
    FreeTextSearchPageRequest request,
    QueryBuilder queryBuilder
  ) {
    Sort sort;
    if (
      request
        .getSortDirection()
        .equals(org.springframework.data.domain.Sort.Direction.ASC)
    ) {
      sort = queryBuilder.sort().byField(request.getSortBy()).createSort();
    } else {
      sort =
        queryBuilder.sort().byField(request.getSortBy()).desc().createSort();
    }
    return sort;
  }

  private Query query(
    FreeTextSearchPageRequest request,
    QueryBuilder queryBuilder
  ) {
    val keywordSearch = queryBuilder
      .keyword()
      .wildcard()
      .onField("searchableHebrewText")
      .matching("*" + request.getSearchTerm() + "*")
      .createQuery();
    val phraseSearch = queryBuilder
      .phrase()
      .onField("searchableHebrewText")
      .sentence("*" + request.getSearchTerm() + "*")
      .createQuery();
    val matches = queryBuilder
      .bool()
      .should(keywordSearch)
      .should(phraseSearch)
      .createQuery();
    val isHit = queryBuilder.bool().must(matches);
    addPathFilters(request, queryBuilder, isHit);
    return isHit.createQuery();
  }

  private void addPathFilters(
    FreeTextSearchPageRequest request,
    QueryBuilder queryBuilder,
    MustJunction baseQuery
  ) {
    if (request.getCustomArgs().containsKey("validPathPrefixes")) {
      List<String> prefixes = request.getCustomArg("validPathPrefixes");
      val hasValidPathPrefix = queryBuilder.bool();
      prefixes
        .stream()
        .map(
          prefix ->
            queryBuilder.phrase().onField("path").sentence(prefix).createQuery()
        )
        .forEach(hasValidPathPrefix::should);
      baseQuery.must(hasValidPathPrefix.createQuery());
    }
  }

  private QueryBuilder queryBuilder(DataManager<Verse> dataManager) {
    FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(
      dataManager.entityManager()
    );
    return fullTextEntityManager
      .getSearchFactory()
      .buildQueryBuilder()
      .forEntity(Verse.class)
      .get();
  }

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void initHibernateSearch() throws InterruptedException {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(
      entityManager
    );
    fullTextEntityManager.createIndexer().startAndWait();
  }
}
