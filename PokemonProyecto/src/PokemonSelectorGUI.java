import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PokemonSelectorGUI extends JFrame {
    private JComboBox<String> typeComboBox;
    private JComboBox<String> pokemonComboBox;
    private JTextField searchField;
    private JTextArea pokemonInfoTextArea;
    private JLabel pokemonImageLabel;
    private static final Logger LOGGER = Logger.getLogger(PokemonSelectorGUI.class.getName());

    private static final String BASE_URL = "https://pokeapi.co/api/v2/";
    private static final Gson gson = new Gson();

    private static final String CONFIG_FILE_PATH = "config.properties";
    private static final Properties config = loadConfig();

    public PokemonSelectorGUI() {
        configureLogger();

        setTitle("Selección de Pokémon por Tipo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);

        JPanel panel = new JPanel(new BorderLayout());

        JPanel comboPanel = new JPanel(new GridLayout(3, 2));

        JLabel searchLabel = new JLabel("Buscar por nombre:");
        searchField = new JTextField();
        comboPanel.add(searchLabel);
        comboPanel.add(searchField);

        searchField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String searchText = searchField.getText();
                    showPokemonInfoName(searchText);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        JLabel typeLabel = new JLabel("Seleccionar tipo:");
        typeComboBox = new JComboBox<>();
        comboPanel.add(typeLabel);
        comboPanel.add(typeComboBox);

        JLabel pokemonLabel = new JLabel("Seleccionar Pokémon:");
        pokemonComboBox = new JComboBox<>();
        comboPanel.add(pokemonLabel);
        comboPanel.add(pokemonComboBox);

        panel.add(comboPanel, BorderLayout.NORTH);

        pokemonInfoTextArea = new JTextArea();
        pokemonInfoTextArea.setEditable(false);
        panel.add(new JScrollPane(pokemonInfoTextArea), BorderLayout.CENTER);

        pokemonImageLabel = new JLabel();
        panel.add(pokemonImageLabel, BorderLayout.SOUTH);

        add(panel);

        loadPokemonData();

        typeComboBox.addActionListener(e -> {
            String selectedType = (String) typeComboBox.getSelectedItem();
            if (selectedType != null) {
                loadPokemonByType(selectedType);
            }
        });

        pokemonComboBox.addActionListener(e -> {
            String selectedPokemon = (String) pokemonComboBox.getSelectedItem();
            if (selectedPokemon != null) {
                showPokemonInfoName(selectedPokemon);
            }
        });
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE_PATH)) {
            properties.load(input);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar el archivo de configuración", e);
        }
        return properties;
    }

    private void configureLogger() {
        try {
            FileHandler fileHandler = new FileHandler("pokemon_selector_log.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al configurar el logger", e);
        }
    }

    private void loadPokemonData() {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                try {
                    URL url = new URL(BASE_URL + "type");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    return gson.fromJson(response.toString(), JsonObject.class);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error al cargar tipos de Pokémon.", ex);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject result = get();
                    if (result != null) {
                        loadPokemonTypes(result);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Error al obtener datos iniciales", ex);
                }
            }
        }.execute();
    }

    private void loadPokemonTypes(JsonObject data) {
        JsonArray types = data.getAsJsonArray("results");
        for (int i = 0; i < types.size(); i++) {
            String typeName = types.get(i).getAsJsonObject().get("name").getAsString();
            typeComboBox.addItem(typeName);
        }
        LOGGER.info("Tipos de Pokémon cargados exitosamente.");
    }

    private void loadPokemonByType(String type) {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                try {
                    URL url = new URL(BASE_URL + "type/" + type);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    return gson.fromJson(response.toString(), JsonObject.class);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error al cargar Pokémon del tipo " + type, ex);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject result = get();
                    if (result != null) {
                        loadPokemonList(result);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Error al obtener Pokémon del tipo " + type, ex);
                }
            }
        }.execute();
    }

    private void loadPokemonList(JsonObject typeData) {
        pokemonComboBox.removeAllItems();  // Limpiar la lista actual de Pokémon

        JsonArray pokemonArray = typeData.getAsJsonArray("pokemon");
        for (int i = 0; i < pokemonArray.size(); i++) {
            String pokemonName = pokemonArray.get(i).getAsJsonObject().get("pokemon").getAsJsonObject().get("name").getAsString();
            pokemonComboBox.addItem(pokemonName);
        }
        LOGGER.info("Pokémon del tipo cargados exitosamente.");
    }


    private void showPokemonInfoName(String searchText) {
        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() {
                if (searchText == null || searchText.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Escriba un Pokémon");
                    return null;
                }
                try {
                    URL url = new URL(BASE_URL + "pokemon/" + searchText.toLowerCase());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    return gson.fromJson(response.toString(), JsonObject.class);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "El Pokémon no existe");
                    LOGGER.info("Texto ingresado: " + searchText);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    JsonObject result = get();
                    if (result != null) {
                        showPokemonInfo(result);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Error al obtener información del Pokémon: " + searchText, ex);
                }
            }
        }.execute();
    }

    private void showPokemonInfo(JsonObject pokemonData) {
        String name = pokemonData.get("name").getAsString();
        int height = pokemonData.get("height").getAsInt();
        int weight = pokemonData.get("weight").getAsInt();

        JsonArray abilitiesArray = pokemonData.getAsJsonArray("abilities");
        String abilities = getAbilitiesString(abilitiesArray);

        JsonArray typesArray = pokemonData.getAsJsonArray("types");
        String types = getTypesString(typesArray);

        JsonArray statsArray = pokemonData.getAsJsonArray("stats");
        String stats = getStatsString(statsArray);

        String frontSpriteUrl = pokemonData.getAsJsonObject("sprites").get("front_default").getAsString();

        String info = "Nombre: " + name + "\n" +
                "Altura: " + height + "\n" +
                "Peso: " + weight + "\n" +
                "Habilidades: " + abilities + "\n" +
                "Tipos: " + types + "\n" +
                "Estadísticas: " + stats + "\n";

        pokemonInfoTextArea.setText(info);
        showPokemonImage(frontSpriteUrl);
    }

    private String getAbilitiesString(JsonArray abilitiesArray) {
        StringBuilder abilities = new StringBuilder();
        for (int i = 0; i < abilitiesArray.size(); i++) {
            String abilityName = abilitiesArray.get(i).getAsJsonObject().getAsJsonObject("ability").get("name").getAsString();
            abilities.append(abilityName);
            if (i < abilitiesArray.size() - 1) {
                abilities.append(", ");
            }
        }
        return abilities.toString();
    }

    private String getTypesString(JsonArray typesArray) {
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < typesArray.size(); i++) {
            String typeName = typesArray.get(i).getAsJsonObject().getAsJsonObject("type").get("name").getAsString();
            types.append(typeName);
            if (i < typesArray.size() - 1) {
                types.append(", ");
            }
        }
        return types.toString();
    }

    private String getStatsString(JsonArray statsArray) {
        StringBuilder stats = new StringBuilder();
        for (int i = 0; i < statsArray.size(); i++) {
            String statName = statsArray.get(i).getAsJsonObject().getAsJsonObject("stat").get("name").getAsString();
            int baseStat = statsArray.get(i).getAsJsonObject().get("base_stat").getAsInt();
            stats.append(statName).append(": ").append(baseStat);
            if (i < statsArray.size() - 1) {
                stats.append(", ");
            }
        }
        return stats.toString();
    }

    private void showPokemonImage(String imageUrl) {
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        URL url = new URL(imageUrl);
                        ImageIcon imageIcon = new ImageIcon(url);
                        Image image = imageIcon.getImage();
                        Image scaledImage = image.getScaledInstance(350, 350, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaledImage);
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error al cargar la imagen del Pokémon", ex);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon result = get();
                    if (result != null) {
                        pokemonImageLabel.setIcon(result);
                        centerImageLabel();
                    } else {
                        pokemonImageLabel.setIcon(null);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Error al obtener la imagen del Pokémon", ex);
                }
            }
        }.execute();
    }

    private void centerImageLabel() {
        pokemonImageLabel.setHorizontalAlignment(JLabel.CENTER);
        pokemonImageLabel.setVerticalAlignment(JLabel.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PokemonSelectorGUI gui = new PokemonSelectorGUI();
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
}
