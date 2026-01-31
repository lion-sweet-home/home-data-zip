package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    List<Apartment> findAllByAptSeqIn(Collection<String> aptSeqs);
}
