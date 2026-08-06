package com.phanileela.controller;

import com.phanileela.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu")
    public ResponseEntity<List<Map<String, Object>>> getAllMenuItems() {
        return ResponseEntity.ok(menuService.getAllMenuItems());
    }

    @GetMapping("/menu/{id}")
    public ResponseEntity<Map<String, Object>> getMenuItemById(@PathVariable String id) {
        return ResponseEntity.ok(menuService.getMenuItemById(id));
    }

    @GetMapping("/menu/category/{categoryId}")
    public ResponseEntity<List<Map<String, Object>>> getMenuByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(menuService.getMenuByCategory(categoryId));
    }

    @GetMapping("/menu/safe")
    public ResponseEntity<List<Map<String, Object>>> getAllergenSafeItems(
            @RequestParam List<String> allergens) {
        return ResponseEntity.ok(menuService.getAllergenSafeItems(allergens));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getAllCategories() {
        return ResponseEntity.ok(menuService.getAllCategories());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(menuService.getStats());
    }

    @PostMapping("/menu")
    public ResponseEntity<Map<String, Object>> addMenuItem(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(menuService.addMenuItem(body));
    }

    @DeleteMapping("/menu/{id}")
    public ResponseEntity<Map<String, Object>> deleteMenuItem(@PathVariable String id) {
        return ResponseEntity.ok(menuService.deleteMenuItem(id));
    }
}
