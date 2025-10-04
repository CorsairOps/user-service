package com.corsairops.corsairopsuserservice.repository;

import com.corsairops.corsairopsuserservice.model.CachedUser;
import org.springframework.data.repository.CrudRepository;

public interface CachedUserRepository extends CrudRepository<CachedUser, String> {

}