package org.your.gossips.gossipsapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.gossipsapp.adapter.out.model.StoredContact;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    @Mapping(source = "contactUsername", target = "username")
    Contact toDto(StoredContact storedContact);
}
