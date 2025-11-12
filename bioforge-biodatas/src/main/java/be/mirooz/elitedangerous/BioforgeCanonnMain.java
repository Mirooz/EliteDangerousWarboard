package be.mirooz.elitedangerous;

import be.mirooz.elitedangerous.biologic.BioSpeciesFactory;
import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.service.BioSpeciesService;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class BioforgeCanonnMain {
    public static void main(String[] args) throws IOException, URISyntaxException {

        List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();
        System.out.println("✅ Total species loaded: " + allSpecies.size());
        allSpecies.forEach(
                e -> {
                    System.out.println(e.getFullName() + " " + e.getVariantMethod() + " " + e.getColorConditionName());
                }
        );

    }

}
