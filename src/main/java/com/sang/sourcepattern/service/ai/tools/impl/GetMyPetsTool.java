package com.sang.sourcepattern.service.ai.tools.impl;

import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.PetService;
import com.sang.sourcepattern.service.ai.tools.AITool;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetMyPetsTool implements AITool {

    private final PetService petService;
    private final UserRepository userRepository;

    @Override
    public String getName() { return "get_my_pets"; }

    @Override
    public Set<String> getSupportedAgents() { return Set.of("USER_CHAT"); }

    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
                "name", "get_my_pets",
                "description", "Lấy danh sách thú cưng của người dùng hiện tại với đầy đủ thông tin",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "petId", Map.of("type", "number",
                                        "description", "ID thú cưng cụ thể muốn xem chi tiết (tuỳ chọn)")
                        )
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<PetResponse> allPets = petService.getPetsByOwner(user.getId());
        List<PetResponse> activePets = allPets.stream()
                .filter(PetResponse::isActive)
                .collect(Collectors.toList());

        if (args.get("petId") instanceof Number) {
            int petId = ((Number) args.get("petId")).intValue();
            PetResponse pet = activePets.stream()
                    .filter(p -> p.getId() == petId)
                    .findFirst()
                    .orElse(null);

            if (pet == null) {
                return ToolResult.error("Không tìm thấy thú cưng với ID " + petId);
            }

            return ToolResult.builder()
                    .type("pet_detail")
                    .data(buildPetDetail(pet))
                    .geminiSummary(Map.of("pet", buildPetSummary(pet)))
                    .build();
        }

        // Return all pets
        List<Map<String, Object>> petSummaries = activePets.stream()
                .map(this::buildPetSummary)
                .collect(Collectors.toList());

        List<Map<String, Object>> petDetails = activePets.stream()
                .map(this::buildPetDetail)
                .collect(Collectors.toList());

        return ToolResult.builder()
                .type("pet_list")
                .data(petDetails)
                .geminiSummary(Map.of("count", activePets.size(), "pets", petSummaries))
                .build();
    }

    private Map<String, Object> buildPetSummary(PetResponse pet) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pet.getId());
        m.put("name", pet.getName());
        m.put("species", pet.getSpecies());
        m.put("breed", pet.getBreed());
        m.put("gender", pet.getGender());
        m.put("weight", pet.getWeight());
        m.put("age", calcAge(pet.getDob()));
        m.put("sterilized", pet.isSterilized());
        if (pet.getAllergies() != null) m.put("allergies", pet.getAllergies());
        if (pet.getHealthNote() != null) m.put("healthNote", pet.getHealthNote());
        return m;
    }

    private Map<String, Object> buildPetDetail(PetResponse pet) {
        Map<String, Object> m = new LinkedHashMap<>(buildPetSummary(pet));
        m.put("avatar", pet.getAvatar());
        m.put("color", pet.getColor());
        m.put("dob", pet.getDob() != null ? pet.getDob().toString() : null);
        m.put("favoriteFood", pet.getFavoriteFood());
        m.put("hobbies", pet.getHobbies());
        m.put("walkTime", pet.getWalkTime());
        if (pet.getVaccinations() != null) m.put("vaccinations", pet.getVaccinations());
        if (pet.getReminders() != null) m.put("upcomingReminders", pet.getReminders());
        return m;
    }

    private String calcAge(LocalDate dob) {
        if (dob == null) return "Không rõ";
        Period p = Period.between(dob, LocalDate.now());
        if (p.getYears() > 0) return p.getYears() + " tuổi";
        if (p.getMonths() > 0) return p.getMonths() + " tháng";
        return p.getDays() + " ngày";
    }
}
