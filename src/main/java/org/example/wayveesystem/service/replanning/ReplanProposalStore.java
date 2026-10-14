package org.example.wayveesystem.service.replanning;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.wayveesystem.service.replanning.model.ReplanProposal;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class ReplanProposalStore {

    private final Cache<String, ReplanProposal> proposalCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public void save(ReplanProposal proposal) {
        if (proposal != null && proposal.getProposalId() != null) {
            proposalCache.put(proposal.getProposalId(), proposal);
        }
    }

    public Optional<ReplanProposal> findById(String proposalId) {
        if (proposalId == null) return Optional.empty();
        return Optional.ofNullable(proposalCache.getIfPresent(proposalId));
    }

    public void evict(String proposalId) {
        if (proposalId != null) {
            proposalCache.invalidate(proposalId);
        }
    }
}
