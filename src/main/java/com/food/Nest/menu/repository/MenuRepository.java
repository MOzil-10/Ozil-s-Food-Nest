package com.food.Nest.menu.repository;

import com.food.Nest.menu.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByAvailableTrue();

    List<MenuItem> findByCategoryAndAvailableTrue(String category);

    List<MenuItem> findByNameContainingIgnoreCaseAndAvailableTrue(String name);
}
