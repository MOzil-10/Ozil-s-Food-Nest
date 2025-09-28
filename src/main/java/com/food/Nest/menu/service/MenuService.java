package com.food.Nest.menu.service;


import com.food.Nest.menu.model.MenuItem;
import com.food.Nest.menu.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MenuService {

    @Autowired
    private MenuRepository menuItemRepository;

    /**
     * Get all available menu items
     */
    public List<MenuItem> getAllAvailableMenuItems() {
        return menuItemRepository.findByAvailableTrue();
    }

    /**
     * Get menu items by category
     */
    public List<MenuItem> getMenuItemsByCategory(String category) {
        return menuItemRepository.findByCategoryAndAvailableTrue(category);
    }

    /**
     * Search menu items by name
     */
    public List<MenuItem> searchMenuItems(String searchTerm) {
        return menuItemRepository.findByNameContainingIgnoreCaseAndAvailableTrue(searchTerm);
    }

    /**
     * Get menu item by ID
     */
    public Optional<MenuItem> getMenuItem(Long id) {
        return menuItemRepository.findById(id);
    }

    /**
     * Create new menu item
     */
    public MenuItem createMenuItem(MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    /**
     * Update menu item
     */
    public MenuItem updateMenuItem(Long id, MenuItem updatedMenuItem) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));

        existing.setName(updatedMenuItem.getName());
        existing.setDescription(updatedMenuItem.getDescription());
        existing.setPrice(updatedMenuItem.getPrice());
        existing.setCategory(updatedMenuItem.getCategory());
        existing.setAvailable(updatedMenuItem.getAvailable());

        return menuItemRepository.save(existing);
    }

    /**
     * Delete menu item (soft delete by setting available to false)
     */
    public void deleteMenuItem(Long id) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));

        existing.setAvailable(false);
        menuItemRepository.save(existing);
    }
}