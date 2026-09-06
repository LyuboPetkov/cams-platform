package cams.repository;

import cams.entity.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Substring match, but ordered by how well the match fits, because the caller
    // keeps only the first few rows. Without the ORDER BY, a short query that also
    // occurs inside longer, unrelated names ("git" inside "digital ...") can push
    // the exact skill out of the truncated result entirely - and the order of an
    // unsorted query is not guaranteed to be stable between calls either.
    // Rank: exact name, then name starting with the query, then the query at the
    // start of any later word, then anything else. Length and name break ties, so
    // the same query always returns the same rows in the same order.
    @Query("""
            SELECT s FROM Skill s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY
                CASE
                    WHEN LOWER(s.name) = LOWER(:search) THEN 0
                    WHEN LOWER(s.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1
                    WHEN LOWER(s.name) LIKE LOWER(CONCAT('% ', :search, '%')) THEN 2
                    ELSE 3
                END,
                LENGTH(s.name),
                s.name
            """)
    List<Skill> searchByNameRanked(@Param("search") String search, Pageable pageable);

}
