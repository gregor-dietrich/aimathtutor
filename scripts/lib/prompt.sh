#!/bin/bash

prompt_yes_no() {
    local question="$1"
    local default_answer="$2"

    if [ -z "$question" ]; then
        question="Proceed"
    fi

    if [ -z "$default_answer" ]; then
        default_answer="n"
    fi

    case "$default_answer" in
        [yYjJ])
            question="${question}? [Y/n]: "
            ;;
        *)
            question="${question}? [y/N]: "
            ;;
    esac

    read -r -p "${question}" reply
    if [ -z "$reply" ] && [ -n "$default_answer" ]; then
        reply="$default_answer"
    fi
    case "$reply" in
        [yYjJ])
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}
