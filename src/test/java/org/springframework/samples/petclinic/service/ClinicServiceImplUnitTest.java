package org.springframework.samples.petclinic.service;
import org.springframework.dao.EmptyResultDataAccessException;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.Owner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.samples.petclinic.repository.SpecialtyRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.samples.petclinic.repository.VisitRepository;

// Imports for test number 6
import org.springframework.samples.petclinic.model.Pet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplUnitTest {

    @Mock
    private PetRepository petRepository;
    @Mock
    private VetRepository vetRepository;
    @Mock
    private OwnerRepository ownerRepository; //fake
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private SpecialtyRepository specialtyRepository;
    @Mock
    private PetTypeRepository petTypeRepository;

    @InjectMocks
    private ClinicServiceImpl clinicService; //real

    @Test
    void findOwners_whenLastNameIsNull_shouldSearchAllOwners() {

        // A normal paging request: first page, 5 owners per page.
        Pageable pageable = PageRequest.of(0, 5);

        // A fake result.
        Page<Owner> fakeResultFromRepository = new PageImpl<>(List.of(new Owner(), new Owner()));

        // If someone calls findAll(pageable), return that fake result.
        when(ownerRepository.findAll(pageable)).thenReturn(fakeResultFromRepository);

        // Call the real service method with no last name (null).
        Page<Owner> actualResult = clinicService.findOwners(null, pageable);

        // 1. The service returned exactly what the repository gave it.
        assertThat(actualResult).isSameAs(fakeResultFromRepository);

        // 2. It asked the repository for ALL owners.
        verify(ownerRepository).findAll(pageable);

        // 3. It did not ask for owners filtered by last name.
        verify(ownerRepository, never()).findByLastName(any(), any());
    }

    @Test
    void findOwners_whenLastNameIsGiven_shouldSearchByLastName() {

        Pageable pageable = PageRequest.of(0, 5);

        Page<Owner> fakeResultFromRepository = new PageImpl<>(List.of(new Owner()));

        when(ownerRepository.findByLastName("Davis", pageable)).thenReturn(fakeResultFromRepository);

        // Call the real service method WITH a last name.
        Page<Owner> actualResult = clinicService.findOwners("Davis", pageable);

        // 1. The service returned exactly what the repository gave it.
        assertThat(actualResult).isSameAs(fakeResultFromRepository);

        // 2. It searched by last name.
        verify(ownerRepository).findByLastName("Davis", pageable);

        // 3. It did NOT fetch all owners.
        verify(ownerRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findOwners_whenLastNameIsEmptyString_shouldStillSearchByLastName() {

        Pageable pageable = PageRequest.of(0, 5);

        // An empty page - no owners have an empty last name.
        Page<Owner> emptyResult = new PageImpl<>(List.<Owner>of());

        when(ownerRepository.findByLastName("", pageable)).thenReturn(emptyResult);

        // Call the real method with an EMPTY STRING, not null.
        Page<Owner> actualResult = clinicService.findOwners("", pageable);

        // The empty string is not null, so the code takes the FILTERED branch.
        verify(ownerRepository).findByLastName("", pageable);

        // It does NOT treat an empty string as "no filter".
        verify(ownerRepository, never()).findAll(any(Pageable.class));

        assertThat(actualResult).isEmpty();
    }

    @Test
    void findOwnerById_whenOwnerExists_shouldReturnTheOwner() {

        Owner existingOwner = new Owner();
        when(ownerRepository.findById(1)).thenReturn(existingOwner);

        Owner actualResult = clinicService.findOwnerById(1);

        // The owner came back unchanged
        assertThat(actualResult).isSameAs(existingOwner);

        // and the correct id was passed down to the repository.
        verify(ownerRepository).findById(1);
    }

    @Test
    void findOwnerById_whenOwnerDoesNotExist_shouldReturnNullInsteadOfThrowing() {

        // This time the fake repository does NOT return anything -
        // it throws the exception a real database throws when nothing is found.
        when(ownerRepository.findById(999))
            .thenThrow(new EmptyResultDataAccessException(1));

        Owner actualResult = clinicService.findOwnerById(999);

        // The service caught the exception and returned null
        // instead of letting it escape to the caller.
        assertThat(actualResult).isNull();
    }

    @Test
    void savePet_whenPetHasNoType_shouldThrowNullPointerException() {

        Pet petWithoutType = new Pet();

        assertThatThrownBy(() -> clinicService.savePet(petWithoutType))
            .isInstanceOf(NullPointerException.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

}
