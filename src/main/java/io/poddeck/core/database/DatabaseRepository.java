package io.poddeck.core.database;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.scheduling.annotation.Async;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@NoRepositoryBean
public interface DatabaseRepository<T, ID extends Serializable> extends Repository<T, ID> {
  @Async
  CompletableFuture<Optional<T>> findById(ID id);

  @Async
  CompletableFuture<Boolean> existsById(ID id);

  @Async
  CompletableFuture<List<T>> findAll();

  @Async
  CompletableFuture<Void> save(T entity);

  @Async
  CompletableFuture<Void> deleteById(ID id);

  @Async
  CompletableFuture<Void> delete(T entity);
}