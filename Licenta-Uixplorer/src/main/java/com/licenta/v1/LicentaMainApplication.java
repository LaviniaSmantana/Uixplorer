package com.licenta.v1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import py4j.GatewayServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;


@SpringBootApplication
public class LicentaMainApplication {

	private static Process process;

	public static void main(String[] args) {
		
		SpringApplication.run(LicentaMainApplication.class, args);

		String pythonScriptPath = "ml-recommendation.py";
		ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScriptPath);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (process != null) {
				process.descendants().forEach(ProcessHandle::destroy);
				process.destroy();
				try {
					if (!process.waitFor(2, TimeUnit.SECONDS)) {
						process.destroyForcibly();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Python script stopped.");
			}
		}));

		Thread pythonThread = new Thread(() -> {
			try {
				process = processBuilder.start();

				BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				String s;
				System.out.println("Standard output:");
				while ((s = stdInput.readLine()) != null) {
					System.out.println(s);
				}

				System.out.println("Error output:");
				while ((s = stdError.readLine()) != null) {
					System.out.println(s);
				}

				int exitCode = process.waitFor();
				System.out.println("Exited with code: " + exitCode);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		pythonThread.start();
	}
}
