package com.kfels.shorturl.repo;

import org.springframework.data.repository.CrudRepository;

import com.kfels.shorturl.entity.Datalog;

public interface DatalogRepo extends CrudRepository<Datalog, Integer> {

}
