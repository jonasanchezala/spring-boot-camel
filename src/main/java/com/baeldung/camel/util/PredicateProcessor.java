package com.baeldung.camel.util;

import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;

import static org.apache.camel.builder.Builder.header;

public class PredicateProcessor {

    public static Predicate isValidGetPredicate(){

        Predicate isValidPredicate = PredicateBuilder.and(
                header("userName").isNotNull(),
                header("password").isNotNull(),
                PredicateBuilder.or(
                        header("channel").isEqualTo("atm"),
                        header("channel").isEqualTo("officeAtm"),
                        header("channel").isEqualTo("phone"),
                        header("channel").isEqualTo("web"),
                        header("channel").isEqualTo("mobileApp")),
                header("reference").isNotNull()
        );

        return isValidPredicate;
    }

}
