package be.mirooz.elitedangerous.siriuscorp;

import be.mirooz.elitedangerous.siriuscorp.client.SiriuscorpClient;

import java.io.IOException;

public class SiriuscorpMain {
    public static void main(String[] args) throws IOException {

        SiriuscorpClient siriuscorpClient = new SiriuscorpClient();
        System.out.println(siriuscorpClient.fetchConflictSystems("Luyten's Star"));
    }
}