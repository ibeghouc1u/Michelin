# Guide Michelin - API REST Backend

**Projet de Développement Web Services (Master 2)**  
**Réalisé par :** IBEGHOUCHENE Juba  

---

## 📌 Présentation du Projet

Ce projet consiste en la réalisation d'une API REST complète pour un guide gastronomique type "Guide Michelin". L'application permet de gérer des restaurants, d'ajouter des avis et des évaluations avec notes, de calculer automatiquement la moyenne des étoiles, de rechercher des avis par mots-clés, de stocker des photos dans un stockage objet distant, et de sécuriser les accès via OpenID Connect (Keycloak).

---

## 🛠️ Technologies Utilisées

* **Java 21** & **Spring Boot 3 / 4** (Spring Web, Spring Data JPA, Spring Security, Spring Validation)
* **H2 Database** : Base de données locale relationnelle avec console H2.
* **Apache Lucene (9.12.0)** : Moteur d'indexation et de recherche plein texte pour les commentaires d'avis.
* **Keycloak & OAuth2 / OpenID Connect** : Authentification et gestion des rôles (USER, ADMIN) via tokens JWT.
* **MinIO / AWS SDK S3 v2 (`software.amazon.awssdk:s3`)** : Stockage objet pour les photos de couverture et de plats via URLs présignées.
* **SpringDoc OpenAPI / Swagger UI** : Documentation interactive automatique de l'API.
* **JUnit 5 & Mockito** : Tests unitaires de la couche service.
* **Bruno** : Client API pour les tests d'intégration et les scénarios de requêtes.

---

## 📖 Documentation de l'API (Swagger UI)

Lorsque l'application est lancée localement, la documentation OpenAPI interactive est directement accessible dans le navigateur aux adresses suivantes :

* **Interface Swagger UI :** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Spécification OpenAPI (JSON) :** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

L'interface Swagger permet de voir la liste exhaustive des routes, les statuts HTTP retournés (200, 201, 204, 400, 403, 404), ainsi que la structure exacte des DTOs en entrée et en sortie.

---

## 📸 Captures d'Écran des Tests Bruno

Pour prouver le bon fonctionnement de toutes les routes de l'API, j'ai réalisé une série complète de tests avec **Bruno**.  
L'endroit idéal pour stocker ces preuves dans le dépôt Git est le dossier :  
📂 **`docs/screenshots/`**

Vous y trouverez les captures des tests suivants :
1. `01-create-restaurant-admin-201.png` : Création d'un restaurant avec le compte ADMIN Noël (201 Created).
2. `02-create-restaurant-user-forbidden-403.png` : Refus de création d'un restaurant par Lucien USER (403 Forbidden).
3. `03-get-restaurant-moyenne-minus-one.png` : Vérification de la moyenne à `-1.0` si le restaurant n'a pas d'avis.
4. `04-add-evaluation-user-201.png` : Ajout d'une évaluation par Lucien avec note et commentaire (201 Created).
5. `05-get-restaurant-calculated-moyenne.png` : Calcul dynamique de la moyenne après plusieurs avis.
6. `06-lucene-search-evaluations.png` : Recherche plein texte de commentaires via Apache Lucene.
7. `07-get-my-evaluations.png` : Route `GET /evaluations/me` retournant uniquement les avis de l'utilisateur connecté.
8. `08-delete-evaluation-forbidden-403.png` : Refus de suppression de l'avis d'autrui sans être admin (403 Forbidden).
9. `09-s3-presigned-url-generation.png` : Génération du lien d'upload S3 pour la photo.
10. `10-s3-upload-curl-success-200.png` : Upload réussi de l'image du tiramisu avec réponse HTTP 200 OK.

---

## 🏗️ Choix d'Architecture et Explications par Couche

J'ai structuré l'application selon une architecture 3-tiers standard, en respectant les bonnes pratiques vues en cours.

### 1. Entités (`entity`) vs DTOs (`dto`)
J'ai choisi de séparer strictement la base de données (`Entity`) et les objets échangés avec l'extérieur (`DTO`) :
* **Entités (`RestaurantEntity`, `EvaluationEntity`)** : Représentent les tables relationnelles dans H2 avec JPA / Hibernate.
* **DTOs de Requête (`RestaurantRequestDto`, `EvaluationRequestDto`)** : Utilisent Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Min`, `@Max`) pour valider les données en entrée dès la réception de la requête.
* **DTOs de Réponse (`RestaurantResponseDto`, `EvaluationResponseDto`)** : Permettent d'éviter le problème des boucles infinies de sérialisation JSON (`OneToMany` / `ManyToOne`) et d'ajouter des champs calculés à la volée comme la moyenne des notes.

### 2. Les méthodes de conversion `mapToDto`
Dans mes services, j'ai implémenté des méthodes privées `mapToResponseDto` et `mapEvaluationToDto`.  
Elles permettent de transformer une entité issue de la base de données en DTO de sortie propre, en calculant la note moyenne et en formatant la liste des avis.

### 3. Couche Repository (`repository`)
J'ai utilisé des interfaces qui étendent `JpaRepository` (`RestaurantRepository`, `EvaluationRepository`).  
Pour les évaluations, j'ai ajouté la méthode dérivée :
`List<EvaluationEntity> findByAuthorId(String authorId)`  
Elle permet à Spring Data JPA de générer automatiquement la requête SQL pour récupérer les évaluations de l'utilisateur connecté sur la route `/evaluations/me`.

### 4. Couche Service (`service`)
C'est ici que se trouve toute la logique métier :
* **`RestaurantService`** :
  * Gestion du CRUD des restaurants.
  * Méthode `calculateAverage()` : applique la règle stricte du sujet en renvoyant `-1.0` si le restaurant ne possède aucune évaluation, ou la moyenne arithmétique de ses notes.
* **`EvaluationService`** :
  * Ajout d'une évaluation et indexation automatique du commentaire dans Lucene.
  * Suppression d'une évaluation : vérifie si l'utilisateur est soit l'auteur de l'avis (`authorId`), soit un ADMIN. Si ce n'est pas le cas, elle lève une `AccessDeniedException`.
  * Recherche plein texte : appelle Lucene pour obtenir les IDs correspondants puis recharge les entités depuis le repository.
* **`LuceneService`** :
  * Initialise l'index dans le dossier `target/lucene-index`.
  * Utilise `IndexWriter` avec `StandardAnalyzer` pour indexer les commentaires, et `IndexSearcher` avec `QueryParser` pour la recherche.
* **`S3Service`** :
  * Utilise le `S3Presigner` du SDK AWS v2 pour générer les liens d'upload temporaires signés vers MinIO.

### 5. Couche Contrôleur (`controller`)
* **`RestaurantController`** : Expose les routes `/restaurants`. Les méthodes d'écriture (`POST`, `PUT`, `POST image-upload-url`) sont protégées par `@PreAuthorize("hasRole('ADMIN')")`. Les méthodes de lecture (`GET`) sont publiques.
* **`EvaluationController`** : Expose les routes d'évaluation. Utilise `Authentication principal` pour récupérer le nom d'utilisateur connecté extrait du JWT Keycloak.

### 6. Gestion globale des erreurs (`GlobalExceptionHandler`)
J'ai créé un intercepteur d'erreurs avec `@RestControllerAdvice` qui capture les exceptions pour renvoyer un format d'erreur JSON uniforme `{ "code": <int>, "message": "<string>" }` et masquer les stacktraces :
* `404 NOT_FOUND` : Quand un ID de restaurant ou d'évaluation n'existe pas (`ResourceNotFoundException`).
* `400 BAD_REQUEST` : En cas d'erreur de validation des données DTO (`MethodArgumentNotValidException`).
* `403 FORBIDDEN` : En cas de tentative d'accès sans le rôle approprié (`AccessDeniedException`, `AuthorizationDeniedException`).
* `500 INTERNAL_SERVER_ERROR` : Pour toute autre erreur imprévue.

### 7. Sécurité OpenID Connect (`SecurityConfig`)
* Utilise `spring-boot-starter-oauth2-resource-server` configuré sur le Keycloak distant (`https://keycloak.bober.ovh/realms/master`).
* J'ai écrit un `JwtAuthenticationConverter` personnalisé pour extraire les rôles situés dans `realm_access.roles` et `resource_access.*.roles`, et les convertir en `ROLE_ADMIN` et `ROLE_USER` utilisables par `@PreAuthorize`.

---

## ☁️ Fonctionnalité Stockage d'Images S3 (MinIO)

Conformément à ce qui a été expliqué en cours, j'ai fait le choix de **ne pas faire "passe-plat"** avec l'API Spring Boot (ce qui surchargerait la mémoire et la bande passante du serveur).  
J'ai donc mis en place l'architecture par **URLs présignées** :
1. Le client demande une URL d'upload au backend (`POST /restaurants/{id}/image-upload-url` ou `POST /evaluations/{id}/photo-upload-url`).
2. L'API génère une URL temporaire signée via `S3Presigner` (valable 10 minutes) pour les buckets `restaurant` ou `evaluation` sur `https://s3.bober.ovh`.
3. L'API stocke le lien permanent final dans l'entité en base H2.
4. Le client effectue un `HTTP PUT` directement vers le serveur MinIO pour déposer le fichier binaire.

### ⚠️ Problème rencontré avec Bruno lors de l'upload et résolution :
Lors du test de l'upload direct avec le client Bruno, j'ai rencontré l'erreur suivante renvoyée par MinIO :
```xml
<Error>
    <Code>AuthorizationQueryParametersError</Code>
    <Message>The authorization query parameters that you provided are not valid.</Message>
</Error>
```

**Analyse du problème :**  
En analysant les requêtes, j'ai constaté que lorsque l'on colle l'URL présignée dans Bruno, celui-ci découpe automatiquement les paramètres dans son onglet *Params* et ré-encode les caractères déjà encodés (par exemple en transformant les `%2F` de `X-Amz-Credential` en `%252F`). De plus, Bruno conservait parfois par héritage le header `Authorization: Bearer <token>`, ce qui provoquait un conflit de signature chez MinIO.

**Validation réussie via le Terminal :**  
Pour contourner l'altération causée par Bruno et valider mon code Java, j'ai exécuté la requête `PUT` brute directement via le terminal avec `curl.exe` :
```powershell
curl.exe -i -X PUT -T "chemin/tiramisu.jpg" "https://s3.bober.ovh/evaluation/eval-4-...?...signature..."
```
Le serveur MinIO a immédiatement répondu :
```text
HTTP/1.1 200 OK
etag: "027593a26bf13c7f782667f221f34d64"
```
Ce test a confirmé à 100% que l'URL présignée générée par mon service Java était parfaitement valide et que l'image a bien été enregistrée sur le serveur distant S3.

---

## 🧪 Tests Unitaires

J'ai couvert la logique métier avec des tests unitaires Mockito / JUnit 5 dans `src/test/java/` :
* **`RestaurantServiceTest`** :
  * Cas limite : vérification que la moyenne renvoie `-1.0` si le restaurant n'a aucun avis.
  * Calcul mathématique de la moyenne avec plusieurs avis.
  * Levée de l'exception 404 si le restaurant n'existe pas.
  * Création et mise à jour de restaurant.
* **`EvaluationServiceTest`** :
  * Ajout d'évaluation et appel de l'indexation Lucene.
  * Contrôle de sécurité : l'auteur peut supprimer son avis, un utilisateur non-admin reçoit une `AccessDeniedException`.
  * Filtre de "mes évaluations" par `authorId`.

Pour lancer l'ensemble des tests :
```powershell
.\mvnw.cmd test
```

---

## 🚀 Démarrage du Projet

```powershell
# Cloner le dépôt et se placer dans le projet
cd Michelin

# Lancer l'application
.\mvnw.cmd spring-boot:run
```
L'API démarre sur le port `8080`.
Console H2 disponible sur : `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:./michelin`, user: `root`, password: `root`).
Documentation Swagger disponible sur : `http://localhost:8080/swagger-ui.html`.

