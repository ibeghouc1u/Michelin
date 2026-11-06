package com.example.michelin.service;

import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LuceneService {

    private final IndexWriter indexWriter;
    private final QueryParser commentQueryParser;

    public LuceneService() {
        try {
            Path path = Paths.get("target/lucene-index");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            Directory index = FSDirectory.open(path);
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            this.indexWriter = new IndexWriter(index, indexWriterConfig);
            this.commentQueryParser = new QueryParser("comment", analyzer);
            this.commentQueryParser.setAllowLeadingWildcard(true);
        } catch (IOException e) {
            throw new RuntimeException("Erreur initialisation Lucene", e);
        }
    }

    // Indexation d'un commentaire
    public void indexComment(Long evaluationId, String comment) {
        try {
            Document document = new Document();
            document.add(new StringField("id", String.valueOf(evaluationId), Field.Store.YES));
            document.add(new TextField("comment", comment != null ? comment : "", Field.Store.YES));
            this.indexWriter.addDocument(document);
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Erreur écriture Lucene", e);
        }
    }

    // Suppression d'un commentaire de l'index
    public void deleteComment(Long evaluationId) {
        try {
            this.indexWriter.deleteDocuments(new Term("id", String.valueOf(evaluationId)));
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException("Erreur suppression Lucene", e);
        }
    }

    // Recherche de mots-clés dans les commentaires
    public List<Long> searchComments(String textToSearch) {
        if (textToSearch == null || textToSearch.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try (DirectoryReader reader = DirectoryReader.open(this.indexWriter)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = commentQueryParser.parse(textToSearch.trim());
            TopDocs topDocs = searcher.search(query, 50);

            return Arrays.stream(topDocs.scoreDocs)
                    .map(scoreDoc -> {
                        try {
                            String idStr = searcher.storedFields().document(scoreDoc.doc).getField("id").stringValue();
                            return Long.parseLong(idStr);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (ParseException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Erreur recherche Lucene", e);
        }
    }

    @PreDestroy
    public void close() {
        try {
            if (this.indexWriter != null && this.indexWriter.isOpen()) {
                this.indexWriter.close();
            }
        } catch (IOException ignored) {
        }
    }
}
