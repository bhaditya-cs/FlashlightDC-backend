package org.flashlightdc.flashlight.repository;

import org.flashlightdc.flashlight.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {

    List<Member> findByPartyName(String partyName);

    List<Member> findByState(String state);

    List<Member> findByPartyNameAndState(String partyName, String state);
}