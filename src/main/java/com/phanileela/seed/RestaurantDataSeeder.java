package com.phanileela.seed;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

@Component
public class RestaurantDataSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantDataSeeder.class);
    private final Driver driver;

    public RestaurantDataSeeder(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("Seeding Phani Leela restaurant data into CognoDB...");
            try (Session session = driver.session()) {
                createCategories(session);
                createAllergens(session);
                createIngredients(session);
                createMenuItems(session);
                linkIngredientsToAllergens(session);
                linkMenuItemsToIngredients(session);
                linkPairsWithRelationships(session);
                deleteDummyOrders(session);
            }
            logger.info("Phani Leela data seeded successfully! Restaurant is ready.");
        } catch (Exception e) {
            logger.warn("Could not seed data: {}. Ensure CognoDB is connected and restart.", e.getMessage());
        }
    }

    // ── CATEGORIES ──────────────────────────────────────────────────────────────
    private void createCategories(Session session) {
        List<Map<String, Object>> cats = List.of(
                Map.of("id", "cat-starters",   "name", "Starters",              "icon", "\uD83E\uDD57", "sortOrder", 1),
                Map.of("id", "cat-mainveg",     "name", "Main Course (Veg)",     "icon", "\uD83E\uDEB4", "sortOrder", 2),
                Map.of("id", "cat-mainnonveg",  "name", "Main Course (Non-Veg)", "icon", "\uD83C\uDF57", "sortOrder", 3),
                Map.of("id", "cat-biryani",     "name", "Biryani & Rice",        "icon", "\uD83C\uDF5B", "sortOrder", 4),
                Map.of("id", "cat-breads",      "name", "Breads",                "icon", "\uD83E\uDEB3", "sortOrder", 5),
                Map.of("id", "cat-desserts",    "name", "Desserts",              "icon", "\uD83C\uDF6E", "sortOrder", 6),
                Map.of("id", "cat-beverages",   "name", "Beverages",             "icon", "\uD83E\uDD5B", "sortOrder", 7),
                Map.of("id", "cat-icecream",    "name", "Ice Creams",            "icon", "\uD83C\uDF68", "sortOrder", 8)
        );
        session.run(
                "UNWIND $cats AS c " +
                "MERGE (cat:Category {id: c.id}) " +
                "SET cat.name = c.name, cat.icon = c.icon, cat.sortOrder = c.sortOrder",
                parameters("cats", cats)
        );
        logger.info("Created 7 categories");
    }

    // ── ALLERGENS ────────────────────────────────────────────────────────────────
    private void createAllergens(Session session) {
        List<Map<String, Object>> allergens = List.of(
                Map.of("id", "allergen-gluten",  "name", "Gluten",  "description", "Contains wheat flour or maida"),
                Map.of("id", "allergen-dairy",   "name", "Dairy",   "description", "Contains milk, butter, cream, paneer or yogurt"),
                Map.of("id", "allergen-nuts",    "name", "Nuts",    "description", "Contains cashew, almond or other tree nuts"),
                Map.of("id", "allergen-egg",     "name", "Egg",     "description", "Contains egg"),
                Map.of("id", "allergen-fish",    "name", "Fish",    "description", "Contains fish or seafood")
        );
        session.run(
                "UNWIND $a AS al " +
                "MERGE (allergen:Allergen {id: al.id}) " +
                "SET allergen.name = al.name, allergen.description = al.description",
                parameters("a", allergens)
        );
        logger.info("Created 5 allergens");
    }

    // ── INGREDIENTS ──────────────────────────────────────────────────────────────
    private void createIngredients(Session session) {
        List<Map<String, Object>> ings = List.of(
                // Spices
                Map.of("id", "turmeric",       "name", "Turmeric",       "type", "Spice"),
                Map.of("id", "cumin",          "name", "Cumin",          "type", "Spice"),
                Map.of("id", "coriander",      "name", "Coriander",      "type", "Spice"),
                Map.of("id", "garam-masala",   "name", "Garam Masala",   "type", "Spice"),
                Map.of("id", "red-chili",      "name", "Red Chili",      "type", "Spice"),
                Map.of("id", "kashmiri-chili", "name", "Kashmiri Chili", "type", "Spice"),
                Map.of("id", "cardamom",       "name", "Cardamom",       "type", "Spice"),
                Map.of("id", "cloves",         "name", "Cloves",         "type", "Spice"),
                Map.of("id", "cinnamon",       "name", "Cinnamon",       "type", "Spice"),
                Map.of("id", "bay-leaf",       "name", "Bay Leaf",       "type", "Spice"),
                Map.of("id", "black-pepper",   "name", "Black Pepper",   "type", "Spice"),
                Map.of("id", "mustard-seeds",  "name", "Mustard Seeds",  "type", "Spice"),
                Map.of("id", "saffron",        "name", "Saffron",        "type", "Spice"),
                // Dairy
                Map.of("id", "butter",  "name", "Butter",       "type", "Dairy"),
                Map.of("id", "cream",   "name", "Fresh Cream",  "type", "Dairy"),
                Map.of("id", "milk",    "name", "Milk",         "type", "Dairy"),
                Map.of("id", "paneer",  "name", "Paneer",       "type", "Dairy"),
                Map.of("id", "yogurt",  "name", "Yogurt",       "type", "Dairy"),
                Map.of("id", "ghee",    "name", "Ghee",         "type", "Dairy"),
                // Proteins
                Map.of("id", "chicken", "name", "Chicken", "type", "Protein"),
                Map.of("id", "mutton",  "name", "Mutton",  "type", "Protein"),
                Map.of("id", "fish",    "name", "Fish",    "type", "Protein"),
                Map.of("id", "egg",     "name", "Egg",     "type", "Protein"),
                // Vegetables
                Map.of("id", "tomato",      "name", "Tomato",      "type", "Vegetable"),
                Map.of("id", "onion",       "name", "Onion",       "type", "Vegetable"),
                Map.of("id", "garlic",      "name", "Garlic",      "type", "Vegetable"),
                Map.of("id", "ginger",      "name", "Ginger",      "type", "Vegetable"),
                Map.of("id", "potato",      "name", "Potato",      "type", "Vegetable"),
                Map.of("id", "spinach",     "name", "Spinach",     "type", "Vegetable"),
                Map.of("id", "cauliflower", "name", "Cauliflower", "type", "Vegetable"),
                Map.of("id", "green-peas",  "name", "Green Peas",  "type", "Vegetable"),
                Map.of("id", "capsicum",    "name", "Capsicum",    "type", "Vegetable"),
                // Legumes / Herbs
                Map.of("id", "chickpea",     "name", "Chickpea",     "type", "Legume"),
                Map.of("id", "black-lentil", "name", "Black Lentil", "type", "Legume"),
                Map.of("id", "mint",         "name", "Mint",         "type", "Herb"),
                Map.of("id", "curry-leaves", "name", "Curry Leaves", "type", "Herb"),
                Map.of("id", "coconut-milk", "name", "Coconut Milk", "type", "Liquid"),
                // Grains
                Map.of("id", "rice",      "name", "Basmati Rice", "type", "Grain"),
                Map.of("id", "wheat-flour","name", "Wheat Flour", "type", "Grain"),
                Map.of("id", "maida",     "name", "Maida",        "type", "Grain"),
                Map.of("id", "semolina",  "name", "Semolina",     "type", "Grain"),
                // Nuts & Dry Fruits
                Map.of("id", "cashew",  "name", "Cashew",  "type", "Nut"),
                Map.of("id", "almond",  "name", "Almond",  "type", "Nut"),
                Map.of("id", "raisin",  "name", "Raisin",  "type", "Dried Fruit"),
                // Others
                Map.of("id", "sugar",      "name", "Sugar",       "type", "Sweetener"),
                Map.of("id", "salt",       "name", "Salt",        "type", "Condiment"),
                Map.of("id", "lemon",      "name", "Lemon",       "type", "Citrus"),
                Map.of("id", "oil",        "name", "Cooking Oil", "type", "Fat"),
                Map.of("id", "rose-water", "name", "Rose Water",  "type", "Flavoring"),
                Map.of("id", "mango-pulp", "name", "Mango Pulp",  "type", "Fruit"),
                Map.of("id", "black-tea",  "name", "Black Tea",   "type", "Beverage"),
                Map.of("id", "soda-water", "name", "Soda Water",  "type", "Beverage")
        );
        session.run(
                "UNWIND $ings AS ing " +
                "MERGE (i:Ingredient {id: ing.id}) " +
                "SET i.name = ing.name, i.type = ing.type",
                parameters("ings", ings)
        );
        logger.info("Created {} ingredients", ings.size());
    }

    // ── MENU ITEMS ───────────────────────────────────────────────────────────────
    private void createMenuItems(Session session) {
        List<Map<String, Object>> items = List.of(
            // ── Starters ──
            Map.of("id","dish-001","name","Samosa (2 pcs)","price",120,"isVeg",true,
                "emoji","\uD83E\uDD5F","desc","Crispy pastry filled with spiced potatoes and peas, served with mint chutney","cat","cat-starters"),
            Map.of("id","dish-002","name","Paneer Tikka","price",280,"isVeg",true,
                "emoji","\uD83E\uDDC0","desc","Marinated cottage cheese grilled in a tandoor with bell peppers and onions","cat","cat-starters"),
            Map.of("id","dish-003","name","Chicken Tikka","price",320,"isVeg",false,
                "emoji","\uD83C\uDF57","desc","Tender chicken marinated in yogurt and spices, grilled to perfection in a tandoor","cat","cat-starters"),
            Map.of("id","dish-004","name","Aloo Tikki","price",110,"isVeg",true,
                "emoji","\uD83E\uDE54","desc","Crispy spiced potato patties served with tamarind and mint chutneys","cat","cat-starters"),
            Map.of("id","dish-005","name","Hara Bhara Kabab","price",230,"isVeg",true,
                "emoji","\uD83D\uDFE2","desc","Spinach and potato patties with cashews — healthy and delicious starter","cat","cat-starters"),
            // ── Main Course Veg ──
            Map.of("id","dish-006","name","Paneer Butter Masala","price",320,"isVeg",true,
                "emoji","\uD83C\uDF5B","desc","Soft paneer in a rich creamy tomato-cashew gravy — our signature dish","cat","cat-mainveg"),
            Map.of("id","dish-007","name","Dal Makhani","price",260,"isVeg",true,
                "emoji","\uD83E\uDEB5","desc","Slow-cooked black lentils simmered in butter and cream — a North Indian classic","cat","cat-mainveg"),
            Map.of("id","dish-008","name","Palak Paneer","price",290,"isVeg",true,
                "emoji","\uD83E\uDD6C","desc","Fresh paneer in a velvety spinach gravy, mildly spiced and nutritious","cat","cat-mainveg"),
            Map.of("id","dish-009","name","Chana Masala","price",240,"isVeg",true,
                "emoji","\uD83D\uDFE1","desc","Hearty chickpeas cooked in a tangy spiced tomato-onion gravy","cat","cat-mainveg"),
            Map.of("id","dish-010","name","Aloo Gobi","price",220,"isVeg",true,
                "emoji","\uD83E\uDD66","desc","Potato and cauliflower tossed with turmeric, cumin and aromatic spices","cat","cat-mainveg"),
            // ── Main Course Non-Veg ──
            Map.of("id","dish-011","name","Butter Chicken","price",380,"isVeg",false,
                "emoji","\uD83C\uDF57","desc","Succulent chicken in a velvety tomato-butter sauce — a timeless favourite","cat","cat-mainnonveg"),
            Map.of("id","dish-012","name","Mutton Rogan Josh","price",450,"isVeg",false,
                "emoji","\uD83E\uDD69","desc","Slow-cooked mutton in bold Kashmiri red gravy with whole aromatic spices","cat","cat-mainnonveg"),
            Map.of("id","dish-013","name","Goan Fish Curry","price",360,"isVeg",false,
                "emoji","\uD83D\uDC1F","desc","Fresh fish simmered in a tangy coconut milk curry with coastal spices","cat","cat-mainnonveg"),
            Map.of("id","dish-014","name","Chicken Curry","price",340,"isVeg",false,
                "emoji","\uD83C\uDF72","desc","Classic homestyle chicken curry with rich onion-tomato gravy and warm spices","cat","cat-mainnonveg"),
            // ── Biryani & Rice ──
            Map.of("id","dish-015","name","Chicken Biryani","price",380,"isVeg",false,
                "emoji","\uD83C\uDF5B","desc","Fragrant basmati rice layered with spiced chicken, saffron and caramelised onions","cat","cat-biryani"),
            Map.of("id","dish-016","name","Mutton Biryani","price",450,"isVeg",false,
                "emoji","\uD83C\uDF5B","desc","Tender mutton slow-cooked with aromatic basmati rice and whole spices","cat","cat-biryani"),
            Map.of("id","dish-017","name","Vegetable Biryani","price",280,"isVeg",true,
                "emoji","\uD83C\uDF5A","desc","Seasonal vegetables layered with saffron-flavoured basmati rice and fried onions","cat","cat-biryani"),
            Map.of("id","dish-018","name","Jeera Rice","price",180,"isVeg",true,
                "emoji","\uD83C\uDF5A","desc","Fragrant basmati rice tempered with cumin seeds and ghee — simple and perfect","cat","cat-biryani"),
            // ── Breads ──
            Map.of("id","dish-019","name","Butter Naan","price",60,"isVeg",true,
                "emoji","\uD83E\uDEB3","desc","Soft leavened bread baked in a tandoor and brushed with fresh butter","cat","cat-breads"),
            Map.of("id","dish-020","name","Garlic Naan","price",70,"isVeg",true,
                "emoji","\uD83E\uDDC4","desc","Tandoor-baked bread topped with garlic, butter and fresh coriander","cat","cat-breads"),
            Map.of("id","dish-021","name","Tandoori Roti","price",40,"isVeg",true,
                "emoji","\uD83E\uDEB3","desc","Whole-wheat bread baked directly in a clay tandoor — healthy and rustic","cat","cat-breads"),
            Map.of("id","dish-022","name","Lachha Paratha","price",80,"isVeg",true,
                "emoji","\uD83E\uDEB3","desc","Flaky layered whole-wheat flatbread cooked with ghee on a griddle","cat","cat-breads"),
            // ── Desserts ──
            Map.of("id","dish-023","name","Gulab Jamun (2 pcs)","price",120,"isVeg",true,
                "emoji","\uD83D\uDFE4","desc","Soft milk-solid dumplings soaked in rose-scented sugar syrup","cat","cat-desserts"),
            Map.of("id","dish-024","name","Rasmalai","price",150,"isVeg",true,
                "emoji","\uD83E\uDD5B","desc","Delicate cottage-cheese dumplings floating in saffron-cardamom flavoured milk","cat","cat-desserts"),
            Map.of("id","dish-025","name","Rice Kheer","price",140,"isVeg",true,
                "emoji","\uD83C\uDF5A","desc","Creamy rice pudding with milk, sugar, saffron, almonds and raisins","cat","cat-desserts"),
            Map.of("id","dish-026","name","Jalebi","price",100,"isVeg",true,
                "emoji","\uD83D\uDFE0","desc","Crispy spiral sweets dipped in saffron-cardamom sugar syrup, served warm","cat","cat-desserts"),
            // ── Beverages ──
            Map.of("id","dish-027","name","Sweet Lassi","price",120,"isVeg",true,
                "emoji","\uD83E\uDD5B","desc","Chilled thick yogurt drink sweetened with sugar and flavoured with rose water","cat","cat-beverages"),
            Map.of("id","dish-028","name","Mango Lassi","price",140,"isVeg",true,
                "emoji","\uD83E\uDD6D","desc","Refreshing blend of creamy yogurt and Alphonso mango pulp","cat","cat-beverages"),
            Map.of("id","dish-029","name","Masala Chai","price",60,"isVeg",true,
                "emoji","\u2615","desc","Aromatic spiced Indian tea brewed with ginger, cardamom, cinnamon and cloves","cat","cat-beverages"),
            Map.of("id","dish-030","name","Fresh Lime Soda","price",80,"isVeg",true,
                "emoji","\uD83C\uDF4B","desc","Refreshing fresh lime juice with sweet or salted soda water","cat","cat-beverages"),
            // ── Ice Creams ──
            Map.of("id","dish-031","name","Mango Kulfi","price",120,"isVeg",true,
                "emoji","\uD83E\uDD6D","desc","Authentic Alphonso mango kulfi on a stick garnished with pistachios","cat","cat-icecream"),
            Map.of("id","dish-032","name","Shahi Pistachio Ice Cream","price",140,"isVeg",true,
                "emoji","\uD83C\uDF68","desc","Rich royal green pistachio ice cream infused with cardamom and saffron","cat","cat-icecream"),
            Map.of("id","dish-033","name","Rose Falooda Ice Cream","price",150,"isVeg",true,
                "emoji","\uD83C\uDF67","desc","Chilled rose syrup ice cream served with falooda noodles, basil seeds and nuts","cat","cat-icecream"),
            Map.of("id","dish-034","name","Vanilla Royal Sundae","price",110,"isVeg",true,
                "emoji","\uD83C\uDF68","desc","Classic Madagascar vanilla bean ice cream with caramel & chocolate drizzle","cat","cat-icecream"),
            Map.of("id","dish-035","name","Butterscotch Crunch","price",130,"isVeg",true,
                "emoji","\uD83C\uDF68","desc","Creamy butterscotch ice cream packed with crunchy praline caramel nuts","cat","cat-icecream"),
            // ── New Special Dishes ──
            Map.of("id","dish-036","name","Hyderabadi Chicken Biryani","price",420,"isVeg",false,
                "emoji","\uD83C\uDF5B","desc","Authentic dum-cooked Hyderabadi biryani with juicy chicken, saffron-infused basmati rice, caramelised onions, boiled egg and fresh mint","cat","cat-biryani"),
            Map.of("id","dish-037","name","Special Paneer Biryani","price",340,"isVeg",true,
                "emoji","\uD83C\uDF5B","desc","Premium grilled paneer layered with saffron basmati rice, cashews, caramelised onions and whole aromatic spices — a vegetarian royal feast","cat","cat-biryani"),
            Map.of("id","dish-038","name","Royal Saffron Ice Cream","price",160,"isVeg",true,
                "emoji","\uD83C\uDF68","desc","Luxurious saffron-infused ice cream topped with pistachios, almonds, rose syrup drizzle and real saffron strands — fit for royalty","cat","cat-icecream")
        );
        session.run(
                "UNWIND $items AS item " +
                "MERGE (m:MenuItem {id: item.id}) " +
                "SET m.name = item.name, m.price = item.price, m.isVeg = item.isVeg, " +
                "    m.imageEmoji = item.emoji, m.description = item.desc " +
                "WITH m, item " +
                "MATCH (c:Category {id: item.cat}) " +
                "MERGE (m)-[:IN_CATEGORY]->(c)",
                parameters("items", items)
        );
        logger.info("Created {} menu items", items.size());
    }

    // ── INGREDIENT → ALLERGEN LINKS ───────────────────────────────────────────
    private void linkIngredientsToAllergens(Session session) {
        List<Map<String, Object>> links = List.of(
                Map.of("iid", "wheat-flour", "aid", "allergen-gluten"),
                Map.of("iid", "maida",       "aid", "allergen-gluten"),
                Map.of("iid", "semolina",    "aid", "allergen-gluten"),
                Map.of("iid", "butter",      "aid", "allergen-dairy"),
                Map.of("iid", "cream",       "aid", "allergen-dairy"),
                Map.of("iid", "milk",        "aid", "allergen-dairy"),
                Map.of("iid", "paneer",      "aid", "allergen-dairy"),
                Map.of("iid", "yogurt",      "aid", "allergen-dairy"),
                Map.of("iid", "ghee",        "aid", "allergen-dairy"),
                Map.of("iid", "cashew",      "aid", "allergen-nuts"),
                Map.of("iid", "almond",      "aid", "allergen-nuts"),
                Map.of("iid", "egg",         "aid", "allergen-egg"),
                Map.of("iid", "fish",        "aid", "allergen-fish")
        );
        session.run(
                "UNWIND $links AS lnk " +
                "MATCH (i:Ingredient {id: lnk.iid}), (a:Allergen {id: lnk.aid}) " +
                "MERGE (i)-[:IS_ALLERGEN]->(a)",
                parameters("links", links)
        );
        logger.info("Created 13 ingredient-allergen links");
    }

    // ── MENU ITEM → INGREDIENT LINKS ─────────────────────────────────────────
    private void linkMenuItemsToIngredients(Session session) {
        List<Map<String, Object>> links = List.of(
            // dish-001 Samosa
            Map.of("d","dish-001","i","wheat-flour","a","200g"),
            Map.of("d","dish-001","i","potato","a","150g"),
            Map.of("d","dish-001","i","green-peas","a","50g"),
            Map.of("d","dish-001","i","cumin","a","1 tsp"),
            Map.of("d","dish-001","i","coriander","a","1 tsp"),
            Map.of("d","dish-001","i","ginger","a","1 inch"),
            Map.of("d","dish-001","i","red-chili","a","1 tsp"),
            Map.of("d","dish-001","i","oil","a","for frying"),
            // dish-002 Paneer Tikka
            Map.of("d","dish-002","i","paneer","a","250g"),
            Map.of("d","dish-002","i","yogurt","a","4 tbsp"),
            Map.of("d","dish-002","i","kashmiri-chili","a","2 tsp"),
            Map.of("d","dish-002","i","garam-masala","a","1 tsp"),
            Map.of("d","dish-002","i","ginger","a","1 inch"),
            Map.of("d","dish-002","i","garlic","a","4 cloves"),
            Map.of("d","dish-002","i","lemon","a","1"),
            Map.of("d","dish-002","i","capsicum","a","1"),
            Map.of("d","dish-002","i","onion","a","1"),
            Map.of("d","dish-002","i","oil","a","2 tbsp"),
            // dish-003 Chicken Tikka
            Map.of("d","dish-003","i","chicken","a","400g"),
            Map.of("d","dish-003","i","yogurt","a","4 tbsp"),
            Map.of("d","dish-003","i","kashmiri-chili","a","2 tsp"),
            Map.of("d","dish-003","i","garam-masala","a","1 tsp"),
            Map.of("d","dish-003","i","ginger","a","1 inch"),
            Map.of("d","dish-003","i","garlic","a","4 cloves"),
            Map.of("d","dish-003","i","lemon","a","1"),
            Map.of("d","dish-003","i","oil","a","2 tbsp"),
            // dish-004 Aloo Tikki
            Map.of("d","dish-004","i","potato","a","300g"),
            Map.of("d","dish-004","i","green-peas","a","50g"),
            Map.of("d","dish-004","i","cumin","a","1 tsp"),
            Map.of("d","dish-004","i","red-chili","a","1 tsp"),
            Map.of("d","dish-004","i","wheat-flour","a","2 tbsp"),
            Map.of("d","dish-004","i","oil","a","for frying"),
            // dish-005 Hara Bhara Kabab
            Map.of("d","dish-005","i","spinach","a","200g"),
            Map.of("d","dish-005","i","potato","a","150g"),
            Map.of("d","dish-005","i","cashew","a","20g"),
            Map.of("d","dish-005","i","maida","a","2 tbsp"),
            Map.of("d","dish-005","i","garam-masala","a","1 tsp"),
            Map.of("d","dish-005","i","oil","a","for frying"),
            // dish-006 Paneer Butter Masala
            Map.of("d","dish-006","i","paneer","a","300g"),
            Map.of("d","dish-006","i","butter","a","3 tbsp"),
            Map.of("d","dish-006","i","cream","a","100ml"),
            Map.of("d","dish-006","i","tomato","a","3"),
            Map.of("d","dish-006","i","cashew","a","20g"),
            Map.of("d","dish-006","i","kashmiri-chili","a","2 tsp"),
            Map.of("d","dish-006","i","onion","a","2"),
            Map.of("d","dish-006","i","ginger","a","1 inch"),
            Map.of("d","dish-006","i","garlic","a","4 cloves"),
            Map.of("d","dish-006","i","garam-masala","a","1 tsp"),
            // dish-007 Dal Makhani
            Map.of("d","dish-007","i","black-lentil","a","200g"),
            Map.of("d","dish-007","i","butter","a","3 tbsp"),
            Map.of("d","dish-007","i","cream","a","50ml"),
            Map.of("d","dish-007","i","tomato","a","2"),
            Map.of("d","dish-007","i","onion","a","1"),
            Map.of("d","dish-007","i","ginger","a","1 inch"),
            Map.of("d","dish-007","i","garlic","a","3 cloves"),
            Map.of("d","dish-007","i","garam-masala","a","1 tsp"),
            // dish-008 Palak Paneer
            Map.of("d","dish-008","i","spinach","a","300g"),
            Map.of("d","dish-008","i","paneer","a","200g"),
            Map.of("d","dish-008","i","butter","a","2 tbsp"),
            Map.of("d","dish-008","i","cream","a","50ml"),
            Map.of("d","dish-008","i","onion","a","1"),
            Map.of("d","dish-008","i","ginger","a","1 inch"),
            Map.of("d","dish-008","i","garam-masala","a","1 tsp"),
            Map.of("d","dish-008","i","cumin","a","1 tsp"),
            // dish-009 Chana Masala
            Map.of("d","dish-009","i","chickpea","a","250g"),
            Map.of("d","dish-009","i","tomato","a","2"),
            Map.of("d","dish-009","i","onion","a","2"),
            Map.of("d","dish-009","i","garam-masala","a","1 tsp"),
            Map.of("d","dish-009","i","cumin","a","1 tsp"),
            Map.of("d","dish-009","i","red-chili","a","1 tsp"),
            // dish-010 Aloo Gobi
            Map.of("d","dish-010","i","potato","a","200g"),
            Map.of("d","dish-010","i","cauliflower","a","200g"),
            Map.of("d","dish-010","i","tomato","a","1"),
            Map.of("d","dish-010","i","turmeric","a","1/2 tsp"),
            Map.of("d","dish-010","i","cumin","a","1 tsp"),
            // dish-011 Butter Chicken
            Map.of("d","dish-011","i","chicken","a","400g"),
            Map.of("d","dish-011","i","butter","a","3 tbsp"),
            Map.of("d","dish-011","i","cream","a","100ml"),
            Map.of("d","dish-011","i","tomato","a","3"),
            Map.of("d","dish-011","i","cashew","a","20g"),
            Map.of("d","dish-011","i","kashmiri-chili","a","2 tsp"),
            Map.of("d","dish-011","i","garam-masala","a","1 tsp"),
            Map.of("d","dish-011","i","yogurt","a","3 tbsp"),
            Map.of("d","dish-011","i","ginger","a","1 inch"),
            Map.of("d","dish-011","i","garlic","a","4 cloves"),
            // dish-012 Mutton Rogan Josh
            Map.of("d","dish-012","i","mutton","a","500g"),
            Map.of("d","dish-012","i","yogurt","a","4 tbsp"),
            Map.of("d","dish-012","i","kashmiri-chili","a","3 tsp"),
            Map.of("d","dish-012","i","cardamom","a","3 pods"),
            Map.of("d","dish-012","i","cloves","a","4"),
            Map.of("d","dish-012","i","cinnamon","a","1 stick"),
            Map.of("d","dish-012","i","bay-leaf","a","2"),
            Map.of("d","dish-012","i","onion","a","2"),
            Map.of("d","dish-012","i","ginger","a","2 inch"),
            Map.of("d","dish-012","i","ghee","a","3 tbsp"),
            // dish-013 Goan Fish Curry
            Map.of("d","dish-013","i","fish","a","400g"),
            Map.of("d","dish-013","i","coconut-milk","a","200ml"),
            Map.of("d","dish-013","i","tomato","a","2"),
            Map.of("d","dish-013","i","turmeric","a","1 tsp"),
            Map.of("d","dish-013","i","red-chili","a","2 tsp"),
            Map.of("d","dish-013","i","mustard-seeds","a","1 tsp"),
            Map.of("d","dish-013","i","curry-leaves","a","10 leaves"),
            Map.of("d","dish-013","i","oil","a","2 tbsp"),
            // dish-014 Chicken Curry
            Map.of("d","dish-014","i","chicken","a","400g"),
            Map.of("d","dish-014","i","tomato","a","2"),
            Map.of("d","dish-014","i","onion","a","2"),
            Map.of("d","dish-014","i","yogurt","a","3 tbsp"),
            Map.of("d","dish-014","i","garam-masala","a","1.5 tsp"),
            Map.of("d","dish-014","i","oil","a","3 tbsp"),
            // dish-015 Chicken Biryani
            Map.of("d","dish-015","i","chicken","a","500g"),
            Map.of("d","dish-015","i","rice","a","300g"),
            Map.of("d","dish-015","i","yogurt","a","4 tbsp"),
            Map.of("d","dish-015","i","saffron","a","pinch"),
            Map.of("d","dish-015","i","onion","a","2"),
            Map.of("d","dish-015","i","ghee","a","3 tbsp"),
            Map.of("d","dish-015","i","mint","a","handful"),
            Map.of("d","dish-015","i","milk","a","50ml"),
            Map.of("d","dish-015","i","garam-masala","a","2 tsp"),
            // dish-016 Mutton Biryani
            Map.of("d","dish-016","i","mutton","a","500g"),
            Map.of("d","dish-016","i","rice","a","300g"),
            Map.of("d","dish-016","i","yogurt","a","5 tbsp"),
            Map.of("d","dish-016","i","saffron","a","pinch"),
            Map.of("d","dish-016","i","ghee","a","4 tbsp"),
            Map.of("d","dish-016","i","cardamom","a","3 pods"),
            Map.of("d","dish-016","i","cloves","a","4"),
            Map.of("d","dish-016","i","mint","a","handful"),
            // dish-017 Vegetable Biryani
            Map.of("d","dish-017","i","rice","a","300g"),
            Map.of("d","dish-017","i","potato","a","100g"),
            Map.of("d","dish-017","i","cauliflower","a","100g"),
            Map.of("d","dish-017","i","yogurt","a","3 tbsp"),
            Map.of("d","dish-017","i","saffron","a","pinch"),
            Map.of("d","dish-017","i","ghee","a","2 tbsp"),
            // dish-036 Hyderabadi Chicken Biryani
            Map.of("d","dish-036","i","chicken","a","500g"),
            Map.of("d","dish-036","i","rice","a","350g"),
            Map.of("d","dish-036","i","yogurt","a","5 tbsp"),
            Map.of("d","dish-036","i","saffron","a","pinch"),
            Map.of("d","dish-036","i","onion","a","3"),
            Map.of("d","dish-036","i","ghee","a","4 tbsp"),
            Map.of("d","dish-036","i","mint","a","handful"),
            Map.of("d","dish-036","i","milk","a","50ml"),
            Map.of("d","dish-036","i","garam-masala","a","2 tsp"),
            Map.of("d","dish-036","i","egg","a","2"),
            Map.of("d","dish-036","i","kashmiri-chili","a","2 tsp"),
            // dish-037 Special Paneer Biryani
            Map.of("d","dish-037","i","paneer","a","300g"),
            Map.of("d","dish-037","i","rice","a","300g"),
            Map.of("d","dish-037","i","saffron","a","pinch"),
            Map.of("d","dish-037","i","ghee","a","3 tbsp"),
            Map.of("d","dish-037","i","cashew","a","25g"),
            Map.of("d","dish-037","i","onion","a","2"),
            Map.of("d","dish-037","i","yogurt","a","4 tbsp"),
            Map.of("d","dish-037","i","mint","a","handful"),
            Map.of("d","dish-037","i","garam-masala","a","1.5 tsp"),
            Map.of("d","dish-037","i","cardamom","a","3 pods"),
            // dish-038 Royal Saffron Ice Cream
            Map.of("d","dish-038","i","milk","a","400ml"),
            Map.of("d","dish-038","i","cream","a","200ml"),
            Map.of("d","dish-038","i","saffron","a","pinch"),
            Map.of("d","dish-038","i","almond","a","15g"),
            Map.of("d","dish-038","i","cashew","a","15g"),
            Map.of("d","dish-038","i","cardamom","a","1 tsp"),
            Map.of("d","dish-017","i","mint","a","handful"),
            // dish-018 Jeera Rice
            Map.of("d","dish-018","i","rice","a","200g"),
            Map.of("d","dish-018","i","cumin","a","1 tsp"),
            Map.of("d","dish-018","i","ghee","a","1 tbsp"),
            Map.of("d","dish-018","i","salt","a","to taste"),
            // dish-019 Butter Naan
            Map.of("d","dish-019","i","maida","a","200g"),
            Map.of("d","dish-019","i","yogurt","a","3 tbsp"),
            Map.of("d","dish-019","i","butter","a","2 tbsp"),
            Map.of("d","dish-019","i","sugar","a","1 tsp"),
            // dish-020 Garlic Naan
            Map.of("d","dish-020","i","maida","a","200g"),
            Map.of("d","dish-020","i","yogurt","a","3 tbsp"),
            Map.of("d","dish-020","i","butter","a","2 tbsp"),
            Map.of("d","dish-020","i","garlic","a","4 cloves"),
            // dish-021 Tandoori Roti
            Map.of("d","dish-021","i","wheat-flour","a","200g"),
            Map.of("d","dish-021","i","salt","a","to taste"),
            // dish-022 Lachha Paratha
            Map.of("d","dish-022","i","wheat-flour","a","200g"),
            Map.of("d","dish-022","i","ghee","a","3 tbsp"),
            // dish-023 Gulab Jamun
            Map.of("d","dish-023","i","milk","a","200ml"),
            Map.of("d","dish-023","i","maida","a","50g"),
            Map.of("d","dish-023","i","semolina","a","20g"),
            Map.of("d","dish-023","i","sugar","a","300g"),
            Map.of("d","dish-023","i","rose-water","a","1 tsp"),
            Map.of("d","dish-023","i","ghee","a","for frying"),
            // dish-024 Rasmalai
            Map.of("d","dish-024","i","milk","a","500ml"),
            Map.of("d","dish-024","i","paneer","a","150g"),
            Map.of("d","dish-024","i","sugar","a","150g"),
            Map.of("d","dish-024","i","saffron","a","pinch"),
            Map.of("d","dish-024","i","almond","a","10g"),
            Map.of("d","dish-024","i","rose-water","a","1 tsp"),
            // dish-025 Rice Kheer
            Map.of("d","dish-025","i","milk","a","700ml"),
            Map.of("d","dish-025","i","rice","a","60g"),
            Map.of("d","dish-025","i","sugar","a","100g"),
            Map.of("d","dish-025","i","almond","a","15g"),
            Map.of("d","dish-025","i","raisin","a","15g"),
            Map.of("d","dish-025","i","cardamom","a","3 pods"),
            // dish-026 Jalebi
            Map.of("d","dish-026","i","maida","a","150g"),
            Map.of("d","dish-026","i","yogurt","a","2 tbsp"),
            Map.of("d","dish-026","i","sugar","a","200g"),
            Map.of("d","dish-026","i","ghee","a","for frying"),
            // dish-027 Sweet Lassi
            Map.of("d","dish-027","i","yogurt","a","200ml"),
            Map.of("d","dish-027","i","sugar","a","2 tbsp"),
            Map.of("d","dish-027","i","rose-water","a","1 tsp"),
            // dish-028 Mango Lassi
            Map.of("d","dish-028","i","yogurt","a","150ml"),
            Map.of("d","dish-028","i","mango-pulp","a","100ml"),
            Map.of("d","dish-028","i","sugar","a","1 tbsp"),
            // dish-029 Masala Chai
            Map.of("d","dish-029","i","milk","a","100ml"),
            Map.of("d","dish-029","i","black-tea","a","1 tsp"),
            Map.of("d","dish-029","i","ginger","a","1/2 inch"),
            Map.of("d","dish-029","i","cardamom","a","2 pods"),
            Map.of("d","dish-029","i","sugar","a","1.5 tsp"),
            // dish-030 Fresh Lime Soda
            Map.of("d","dish-030","i","lemon","a","2"),
            Map.of("d","dish-030","i","sugar","a","1 tbsp"),
            Map.of("d","dish-030","i","soda-water","a","200ml"),
            // dish-031 Mango Kulfi
            Map.of("d","dish-031","i","milk","a","100ml"),
            Map.of("d","dish-031","i","mango-pulp","a","100ml"),
            Map.of("d","dish-031","i","sugar","a","2 tbsp"),
            Map.of("d","dish-031","i","almond","a","10g"),
            // dish-032 Shahi Pistachio Ice Cream
            Map.of("d","dish-032","i","milk","a","150ml"),
            Map.of("d","dish-032","i","cream","a","50ml"),
            Map.of("d","dish-032","i","saffron","a","pinch"),
            Map.of("d","dish-032","i","sugar","a","2 tbsp"),
            Map.of("d","dish-032","i","almond","a","15g"),
            // dish-033 Rose Falooda Ice Cream
            Map.of("d","dish-033","i","milk","a","150ml"),
            Map.of("d","dish-033","i","rose-water","a","2 tbsp"),
            Map.of("d","dish-033","i","sugar","a","2 tbsp"),
            Map.of("d","dish-033","i","almond","a","10g"),
            // dish-034 Vanilla Royal Sundae
            Map.of("d","dish-034","i","milk","a","150ml"),
            Map.of("d","dish-034","i","cream","a","50ml"),
            Map.of("d","dish-034","i","sugar","a","2 tbsp"),
            // dish-035 Butterscotch Crunch
            Map.of("d","dish-035","i","milk","a","150ml"),
            Map.of("d","dish-035","i","cream","a","50ml"),
            Map.of("d","dish-035","i","sugar","a","2 tbsp"),
            Map.of("d","dish-035","i","cashew","a","15g")
        );
        session.run(
                "UNWIND $links AS lnk " +
                "MATCH (m:MenuItem {id: lnk.d}), (i:Ingredient {id: lnk.i}) " +
                "MERGE (m)-[:HAS_INGREDIENT {amount: lnk.a}]->(i)",
                parameters("links", links)
        );
        logger.info("Created {} menu-ingredient links", links.size());
    }

    // ── PAIRS WITH RELATIONSHIPS ─────────────────────────────────────────────────
    private void linkPairsWithRelationships(Session session) {
        // These multi-hop relationships power the "Pairs Well With" recommendations
        // e.g. Butter Chicken → PAIRS_WITH → Butter Naan
        List<Map<String, Object>> pairs = List.of(
                Map.of("f","dish-001","t","dish-004"),   // Samosa ↔ Aloo Tikki
                Map.of("f","dish-001","t","dish-027"),   // Samosa → Sweet Lassi
                Map.of("f","dish-002","t","dish-006"),   // Paneer Tikka → Paneer Butter Masala
                Map.of("f","dish-002","t","dish-019"),   // Paneer Tikka → Butter Naan
                Map.of("f","dish-003","t","dish-011"),   // Chicken Tikka → Butter Chicken
                Map.of("f","dish-003","t","dish-015"),   // Chicken Tikka → Chicken Biryani
                Map.of("f","dish-003","t","dish-027"),   // Chicken Tikka → Sweet Lassi
                Map.of("f","dish-004","t","dish-001"),   // Aloo Tikki ↔ Samosa
                Map.of("f","dish-005","t","dish-007"),   // Hara Bhara Kabab → Dal Makhani
                Map.of("f","dish-005","t","dish-017"),   // Hara Bhara Kabab → Veg Biryani
                Map.of("f","dish-006","t","dish-019"),   // Paneer Butter Masala → Butter Naan
                Map.of("f","dish-006","t","dish-020"),   // Paneer Butter Masala → Garlic Naan
                Map.of("f","dish-006","t","dish-027"),   // Paneer Butter Masala → Sweet Lassi
                Map.of("f","dish-007","t","dish-018"),   // Dal Makhani → Jeera Rice
                Map.of("f","dish-007","t","dish-019"),   // Dal Makhani → Butter Naan
                Map.of("f","dish-008","t","dish-019"),   // Palak Paneer → Butter Naan
                Map.of("f","dish-008","t","dish-018"),   // Palak Paneer → Jeera Rice
                Map.of("f","dish-009","t","dish-021"),   // Chana Masala → Tandoori Roti
                Map.of("f","dish-009","t","dish-018"),   // Chana Masala → Jeera Rice
                Map.of("f","dish-010","t","dish-022"),   // Aloo Gobi → Lachha Paratha
                Map.of("f","dish-011","t","dish-019"),   // Butter Chicken → Butter Naan
                Map.of("f","dish-011","t","dish-020"),   // Butter Chicken → Garlic Naan
                Map.of("f","dish-011","t","dish-015"),   // Butter Chicken → Chicken Biryani
                Map.of("f","dish-012","t","dish-016"),   // Mutton Rogan Josh → Mutton Biryani
                Map.of("f","dish-012","t","dish-019"),   // Mutton Rogan Josh → Butter Naan
                Map.of("f","dish-013","t","dish-017"),   // Goan Fish Curry → Veg Biryani
                Map.of("f","dish-013","t","dish-018"),   // Goan Fish Curry → Jeera Rice
                Map.of("f","dish-014","t","dish-018"),   // Chicken Curry → Jeera Rice
                Map.of("f","dish-014","t","dish-021"),   // Chicken Curry → Tandoori Roti
                Map.of("f","dish-015","t","dish-027"),   // Chicken Biryani → Sweet Lassi
                Map.of("f","dish-015","t","dish-028"),   // Chicken Biryani → Mango Lassi
                Map.of("f","dish-016","t","dish-027"),   // Mutton Biryani → Sweet Lassi
                Map.of("f","dish-017","t","dish-027"),   // Veg Biryani → Sweet Lassi
                Map.of("f","dish-018","t","dish-029"),   // Jeera Rice → Masala Chai
                Map.of("f","dish-019","t","dish-027"),   // Butter Naan → Sweet Lassi
                Map.of("f","dish-020","t","dish-028"),   // Garlic Naan → Mango Lassi
                Map.of("f","dish-023","t","dish-029"),   // Gulab Jamun → Masala Chai
                Map.of("f","dish-024","t","dish-029"),   // Rasmalai → Masala Chai
                Map.of("f","dish-025","t","dish-026"),   // Rice Kheer → Jalebi
                Map.of("f","dish-026","t","dish-029")    // Jalebi → Masala Chai
        );
        session.run(
                "UNWIND $pairs AS p " +
                "MATCH (m1:MenuItem {id: p.f}), (m2:MenuItem {id: p.t}) " +
                "MERGE (m1)-[:PAIRS_WITH]->(m2)",
                parameters("pairs", pairs)
        );
        logger.info("Created {} PAIRS_WITH relationships", pairs.size());
    }

    // ── DELETE ALL ORDERS ON STARTUP ─────────────────────────────────────────
    private void deleteDummyOrders(Session session) {
        session.run("MATCH (o:Order) DETACH DELETE o");
        logger.info("Cleared all orders from database — starting fresh with 0 orders");
    }
}
