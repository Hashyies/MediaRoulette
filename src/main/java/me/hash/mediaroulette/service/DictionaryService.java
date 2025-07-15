package me.hash.mediaroulette.service;

import me.hash.mediaroulette.model.Dictionary;
import me.hash.mediaroulette.model.DictionaryAssignment;
import me.hash.mediaroulette.repository.DictionaryRepository;

import java.util.List;
import java.util.Optional;

public class DictionaryService {
    private final DictionaryRepository repository;
    
    public DictionaryService(DictionaryRepository repository) {
        this.repository = repository;
    }
    
    public Dictionary createDictionary(String name, String description, String userId) {
        Dictionary dictionary = new Dictionary(name, description, userId);
        return repository.save(dictionary);
    }
    
    public Optional<Dictionary> getDictionary(String id) {
        return repository.findById(id);
    }
    
    public List<Dictionary> getUserDictionaries(String userId) {
        return repository.findByCreatedBy(userId);
    }
    
    public List<Dictionary> getAccessibleDictionaries(String userId) {
        return repository.findAccessibleDictionaries(userId);
    }
    
    public boolean deleteDictionary(String id, String userId) {
        Optional<Dictionary> dict = repository.findById(id);
        if (dict.isPresent() && dict.get().canBeEditedBy(userId)) {
            return repository.delete(id);
        }
        return false;
    }
    
    public void assignDictionary(String userId, String source, String dictionaryId) {
        DictionaryAssignment assignment = new DictionaryAssignment(userId, source, dictionaryId);
        repository.saveAssignment(assignment);
    }
    
    public boolean unassignDictionary(String userId, String source) {
        return repository.deleteAssignment(userId, source);
    }
    
    public Optional<String> getAssignedDictionary(String userId, String source) {
        return repository.findAssignment(userId, source)
                .map(DictionaryAssignment::getDictionaryId);
    }
    
    public Dictionary updateDictionary(Dictionary dictionary) {
        return repository.save(dictionary);
    }
    
    public boolean updateDictionary(String id, String userId, Dictionary updatedDict) {
        Optional<Dictionary> existing = repository.findById(id);
        if (existing.isPresent() && existing.get().canBeEditedBy(userId)) {
            updatedDict.setId(id);
            repository.save(updatedDict);
            return true;
        }
        return false;
    }

    public String getRandomWordForSource(String userId, String source) {
        System.out.println("DictionaryService: Getting word for user " + userId + " and source " + source);
        Optional<String> dictionaryId = getAssignedDictionary(userId, source);
        System.out.println("DictionaryService: Found dictionary assignment: " + dictionaryId.orElse("none"));
        if (dictionaryId.isPresent()) {
            Optional<Dictionary> dict = repository.findById(dictionaryId.get());
            System.out.println("DictionaryService: Dictionary found: " + dict.isPresent());
            if (dict.isPresent()) {
                String word = dict.get().getRandomWord();
                System.out.println("DictionaryService: Random word from dictionary: " + word);
                repository.incrementDictionaryUsage(dictionaryId.get());
                return word;
            }
        }
        String defaultWord = getDefaultWord();
        System.out.println("DictionaryService: Using default word: " + defaultWord);
        return defaultWord;
    }
    
    private String getDefaultWord() {
        List<Dictionary> defaults = repository.findDefaultDictionaries();
        if (!defaults.isEmpty()) {
            return defaults.get(0).getRandomWord();
        }
        return "random";
    }
}