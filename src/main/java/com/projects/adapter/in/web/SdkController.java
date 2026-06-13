package com.projects.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/sdk")
public class SdkController {

    @GetMapping("/java")
    public Mono<ResponseEntity<String>> getJavaSdkSnippet() {
        String snippet = """
            // Exemple d'intégration Java (HttpClient)
            import java.net.URI;
            import java.net.http.HttpClient;
            import java.net.http.HttpRequest;
            import java.net.http.HttpResponse;
            import java.nio.file.Path;
            import java.io.IOException;
            
            public class VerifIDClient {
                private static final String API_KEY = "votre_cle_api";
                private static final String URL = "https://api.verifid.com/api/documents/upload-analyze";
                
                public static void analyzeDocument(Path frontImagePath) throws IOException, InterruptedException {
                    HttpClient client = HttpClient.newHttpClient();
                    
                    // Note: Il faut construire le multipart form-data. 
                    // Voir la documentation détaillée pour un builder complet Multipart.
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(URL))
                            .header("X-API-KEY", API_KEY)
                            .header("Content-Type", "multipart/form-data; boundary=---boundary")
                            .POST(HttpRequest.BodyPublishers.ofFile(frontImagePath))
                            .build();
                            
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println(response.body());
                }
            }
            """;
        return Mono.just(ResponseEntity.ok(snippet));
    }

    @GetMapping("/python")
    public Mono<ResponseEntity<String>> getPythonSdkSnippet() {
        String snippet = """
            # Exemple d'intégration Python (Requests)
            import requests

            API_KEY = "votre_cle_api"
            URL = "https://api.verifid.com/api/documents/upload-analyze"

            def analyze_document(file_path):
                headers = {
                    "X-API-KEY": API_KEY
                }
                
                with open(file_path, "rb") as f:
                    files = {"frontFile": (file_path, f, "image/jpeg")}
                    response = requests.post(URL, headers=headers, files=files)
                
                if response.status_code == 200:
                    print("Analyse réussie :", response.json())
                else:
                    print("Erreur :", response.text)
                    
            # Utilisation
            # analyze_document("carte_identite.jpg")
            """;
        return Mono.just(ResponseEntity.ok(snippet));
    }
}
