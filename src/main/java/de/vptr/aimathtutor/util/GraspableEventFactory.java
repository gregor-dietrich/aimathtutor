package de.vptr.aimathtutor.util;

import de.vptr.aimathtutor.dto.GraspableEventDto;

/**
 * Factory for creating Graspable Math event DTOs.
 */
public final class GraspableEventFactory {

    private GraspableEventFactory() {}

    /**
     * Creates a math action event with the given parameters.
     *
     * @param eventType
     *            the type of math event
     * @param expressionBefore
     *            the expression before the action
     * @param expressionAfter
     *            the expression after the action
     * @param studentId
     *            the student ID
     * @return the created event
     */
    public static GraspableEventDto createMathActionEvent(final String eventType, final String expressionBefore,
            final String expressionAfter, final Long studentId) {
        final var event = new GraspableEventDto();
        event.eventType = eventType;
        event.expressionBefore = expressionBefore;
        event.expressionAfter = expressionAfter;
        event.studentId = studentId;
        return event;
    }
}
