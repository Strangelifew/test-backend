alter table budget
add author_id integer,
add constraint fk_budget_author
foreign key(author_id)
references author(id);


