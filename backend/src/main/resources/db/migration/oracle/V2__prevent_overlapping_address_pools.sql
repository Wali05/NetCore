-- NetCore does not model separate routing domains, so an address can belong to only one pool.
alter table ip_addresses add constraint uk_ip_address_address unique (address);
