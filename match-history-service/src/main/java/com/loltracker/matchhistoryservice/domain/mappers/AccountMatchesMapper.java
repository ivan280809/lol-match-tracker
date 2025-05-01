package com.loltracker.matchhistoryservice.domain.mappers;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.infrastructure.models.AccountMatchesMO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMatchesMapper {

  AccountMatchesDTO toDTO(AccountMatchesMO entity);

  //Hacer el mapper bien
  default AccountMatchesMO toEntity(AccountMatchesDTO dto){
    return AccountMatchesMO.builder().build();
  }
}
