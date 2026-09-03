package com.travelplatform.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Phase A of the AI Travel Agent (see roadmap): Search & Recommendations.
 * This service holds no travel data of its own — it's a tool-calling
 * orchestrator that turns a natural-language request into calls against the
 * existing Destination/Package/Hotel/Bus/Ride search APIs, then has the LLM
 * compose the results into an answer.
 *
 * Deliberately NOT part of api-gateway: unlike the Booking Aggregator (which
 * just fans out and merges), this owns real orchestration logic (the
 * agentic tool-use loop) and will grow its own concerns — conversation
 * handling, itinerary generation (Phase B), booking confirmation flow
 * (Phase C) — that don't belong bolted onto the Gateway.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TravelAgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelAgentServiceApplication.class, args);
    }
}
