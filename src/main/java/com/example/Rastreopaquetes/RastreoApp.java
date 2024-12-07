package com.example.Rastreopaquetes;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.http.*;
import java.net.URI;
import org.json.*;

public class RastreoApp extends Application {
    // Componentes UI
    private final TextField campoRastreo = new TextField();
    private final TextArea areaResultado = new TextArea();
    private final TextArea areaDetallesHistorial = new TextArea();
    private final ListView<String> listaHistorial;
    private final ProgressIndicator indicadorProgreso = new ProgressIndicator();

    // API y cliente HTTP
    private static final String URL_BASE_API = "http://localhost:5000/api";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();

    public RastreoApp() {
        listaHistorial = new ListView<>(FXCollections.observableArrayList());
    }

    @Override
    public void start(Stage escenario) {
        HBox raiz = crearDisenoPrincipal();
        configurarEscena(escenario, raiz);
        inicializarComponentes();
    }

    private HBox crearDisenoPrincipal() {
        HBox raiz = new HBox(20);
        raiz.setPadding(new Insets(20));

        VBox panelIzquierdo = new VBox(10);
        panelIzquierdo.getChildren().addAll(
                new Label(" APLICACIÓN PARA RASTREAR PAQUETES DE ENVÍO"),
                crearSeccionRastreo()
        );

        VBox panelDerecho = new VBox(10);
        panelDerecho.getChildren().addAll(
                crearSeccionHistorial(),
                crearBarraEstado()
        );

        raiz.getChildren().addAll(panelIzquierdo, new Separator(), panelDerecho);
        return raiz;
    }

    private VBox crearSeccionRastreo() {
        VBox seccion = new VBox(10);
        HBox cajaRastreo = new HBox(10);
        Button botonBuscar = new Button("Consultar");
        botonBuscar.setOnAction(e -> buscarPaquete());

        cajaRastreo.getChildren().addAll(campoRastreo, botonBuscar);
        seccion.getChildren().addAll(new Label("Consulta de Paquetes"), cajaRastreo, areaResultado);
        return seccion;
    }

    private VBox crearSeccionHistorial() {
        VBox seccion = new VBox(10);
        Button botonLimpiar = new Button("Limpiar Historial");
        botonLimpiar.setOnAction(e -> limpiarHistorial());

        seccion.getChildren().addAll(
                new Label("Historial de Consultas"),
                listaHistorial,
                areaDetallesHistorial,
                botonLimpiar
        );
        return seccion;
    }

    private HBox crearBarraEstado() {
        HBox barraEstado = new HBox(10);
        indicadorProgreso.setVisible(false);
        barraEstado.getChildren().addAll(indicadorProgreso);
        return barraEstado;
    }

    private void inicializarComponentes() {
        campoRastreo.setPromptText("Ingrese número de rastreo");
        areaResultado.setEditable(false);
        areaDetallesHistorial.setEditable(false);
        listaHistorial.setOnMouseClicked(e -> mostrarDetallesHistorial());
        obtenerHistorialDesdeAPI();
    }

    private void configurarEscena(Stage escenario, HBox raiz) {
        Scene escena = new Scene(raiz, 900, 400);
        escenario.setScene(escena);
        escenario.setTitle("Sistema de Rastreo de Paquetes");
        escenario.show();
    }

    private void buscarPaquete() {
        String numeroRastreo = campoRastreo.getText().trim();
        if (numeroRastreo.isEmpty()) {
            mostrarError("Por favor ingrese un número de rastreo");
            return;
        }
        enviarSolicitudRastreo(numeroRastreo, true);
    }

    private void enviarSolicitudRastreo(String numeroRastreo, boolean limpiarCampo) {
        indicadorProgreso.setVisible(true);

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE_API + "/track/" + numeroRastreo))
                .GET()
                .build();

        clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .thenAccept(respuesta -> Platform.runLater(() ->
                        procesarRespuesta(respuesta, numeroRastreo, limpiarCampo)))
                .exceptionally(e -> {
                    Platform.runLater(() -> manejarError(e.getMessage()));
                    return null;
                });
    }

    private void procesarRespuesta(HttpResponse<String> respuesta, String numeroRastreo, boolean limpiarCampo) {
        try {
            JSONObject json = new JSONObject(respuesta.body());
            actualizarUI(json, numeroRastreo, limpiarCampo);
        } catch (Exception e) {
            manejarError("Error al procesar la respuesta: " + e.getMessage());
        }
    }

    private void actualizarUI(JSONObject respuesta, String numeroRastreo, boolean limpiarCampo) {
        String resultado = formatearResultadoRastreo(respuesta, numeroRastreo);
        (limpiarCampo ? areaResultado : areaDetallesHistorial).setText(resultado);

        if (limpiarCampo) {
            actualizarHistorial(numeroRastreo);
            campoRastreo.clear();
        }

        indicadorProgreso.setVisible(false);
    }

    private String formatearResultadoRastreo(JSONObject respuesta, String numeroRastreo) {
        StringBuilder resultado = new StringBuilder()
                .append("Número de rastreo: ").append(numeroRastreo).append("\n");

        if (!respuesta.getJSONArray("data").isEmpty()) {
            JSONObject info = respuesta.getJSONArray("data").getJSONObject(0);
            resultado.append("Estado: ").append(info.getString("status")).append("\n")
                    .append("Ubicación: ").append(info.getString("location")).append("\n")
                    .append("Última actualización: ").append(info.getString("last_update"));
        }

        return resultado.toString();
    }

    private void actualizarHistorial(String numeroRastreo) {
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        listaHistorial.getItems().add(0, marca + " - Paquete: " + numeroRastreo);
    }

    private void mostrarDetallesHistorial() {
        String seleccionado = listaHistorial.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            String numeroRastreo = seleccionado.split(" - Paquete: ")[1];
            enviarSolicitudRastreo(numeroRastreo, false);
        }
    }

    private void obtenerHistorialDesdeAPI() {
        indicadorProgreso.setVisible(true);

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE_API + "/history"))
                .GET()
                .build();

        clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .thenAccept(respuesta -> Platform.runLater(() -> {
                    try {
                        JSONObject json = new JSONObject(respuesta.body());
                        JSONArray datosHistorial = json.getJSONArray("history");
                        listaHistorial.getItems().clear();

                        for (int i = 0; i < datosHistorial.length(); i++) {
                            JSONObject entrada = datosHistorial.getJSONObject(i);
                            String marca = entrada.getString("timestamp");
                            String numeroRastreo = entrada.getString("tracking_number");
                            listaHistorial.getItems().add(0, marca + " - Paquete: " + numeroRastreo);
                        }

                        indicadorProgreso.setVisible(false);
                    } catch (Exception e) {
                        manejarError("Error al cargar el historial: " + e.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> manejarError("Error al conectar con el servidor: " + e.getMessage()));
                    return null;
                });
    }

    private void limpiarHistorial() {
        listaHistorial.getItems().clear();
        areaDetallesHistorial.clear();
        areaResultado.clear();
    }

    private void manejarError(String mensaje) {
        mostrarError(mensaje);
        indicadorProgreso.setVisible(false);
    }

    private void mostrarError(String mensaje) {
        new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}